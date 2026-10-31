/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.editor;

import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.JTextComponent;

/**
 * Starts IME reconversion through IMM32, which the JDK's Windows input-method
 * adapter does not expose.  The input context may only be driven from the
 * thread owning the focused window, so the request is posted to that window and
 * performed by a one-shot message hook on the AWT toolkit thread.  The IME then
 * reports the composition through AWT's normal {@code InputMethodEvent} route.
 */
final class WindowsImeReconversion {
	private static final int WM_NULL = 0;
	private static final int PM_REMOVE = 1;
	private static final int WH_GETMESSAGE = 3;
	private static final int SCS_SETRECONVERTSTRING = 0x00010000;
	private static final int SCS_QUERYRECONVERTSTRING = 0x00020000;
	private static final int LANG_JAPANESE = 0x0411;
	private static final int GUI_THREAD_INFO_SIZE = 72;
	private static final long GUI_THREAD_INFO_FOCUS_OFFSET = 16;
	private static final long MSG_SIZE = 48;
	private static final long MSG_MESSAGE_OFFSET = 8;
	/** RECONVERTSTRING: eight DWORDs followed by the UTF-16 string. */
	private static final int RECONVERT_HEADER_SIZE = 32;
	private static final int REQUEST_TIMEOUT_MILLIS = 2000;

	private record Request(JTextComponent editor, String text, int start, int end, Runnable onFailure, Timer timeout) { }

	private static final AtomicReference<Request> pending = new AtomicReference<>();
	private static final AtomicLong activeHook = new AtomicLong();
	/** Touched on the EDT only. */
	private static CancelGuard activeGuard;

	private WindowsImeReconversion() {
	}

	static boolean isWindows() {
		return System.getProperty("os.name", "").startsWith("Windows");
	}

	/**
	 * Whether the focused window's keyboard layout is a Japanese IME.  Lets a
	 * physical acceptance test choose the oracle matching the desktop it runs on.
	 */
	static boolean isJapaneseImeActive() {
		if (!isWindows())
			return false;
		try {
			long focus = focusedWindowOfThisProcess();
			if (focus == 0)
				return false;
			int thread = (int) Native.GET_WINDOW_THREAD_PROCESS_ID.invokeExact(focus, MemorySegment.NULL);
			long layout = (long) Native.GET_KEYBOARD_LAYOUT.invokeExact(thread);
			return (layout & 0xFFFF) == LANG_JAPANESE && (int) Native.IMM_IS_IME.invokeExact(layout) != 0;
		} catch (Throwable unavailable) {
			return false;
		}
	}

	/**
	 * Requests reconversion of {@code [start, end)} in {@code text}.  With an
	 * empty range the IME chooses the clause at the caret.  Must run on the EDT;
	 * {@code onFailure} later runs on the EDT if no reconversion could start.
	 */
	static void request(JTextComponent editor, String text, int start, int end, Runnable onFailure) {
		Request previous = pending.get();
		if (previous != null)
			finish(previous, false);
		removeGuard();
		Timer timeout = new Timer(REQUEST_TIMEOUT_MILLIS, null);
		timeout.setRepeats(false);
		Request request = new Request(editor, text, start, end, onFailure, timeout);
		timeout.addActionListener(event -> finish(request, true));
		pending.set(request);
		activeGuard = new CancelGuard(editor, text, start, end);
		editor.addInputMethodListener(activeGuard);
		try {
			long focus = focusedWindowOfThisProcess();
			if (focus != 0) {
				int thread = (int) Native.GET_WINDOW_THREAD_PROCESS_ID.invokeExact(focus, MemorySegment.NULL);
				activeHook.set((long) Native.SET_WINDOWS_HOOK_EX.invokeExact(WH_GETMESSAGE, Native.HOOK_STUB, 0L, thread));
				if (activeHook.get() != 0 && (int) Native.POST_MESSAGE.invokeExact(focus, Native.REQUEST_MESSAGE, 0L, 0L) != 0) {
					timeout.start();
					return;
				}
			}
		} catch (Throwable unavailable) {
			// Reported below; the caller keeps the text and selection unchanged.
		}
		finish(request, true);
	}

	/** Ends {@code request} unless a newer one has replaced it, and optionally reports its failure. */
	private static void finish(Request request, boolean failed) {
		if (!pending.compareAndSet(request, null))
			return;
		unhook();
		request.timeout().stop();
		if (failed) {
			removeGuard();
			request.onFailure().run();
		}
	}

	private static void removeGuard() {
		if (activeGuard != null) {
			activeGuard.editor.removeInputMethodListener(activeGuard);
			activeGuard = null;
		}
	}

	private static void unhook() {
		long hook = activeHook.getAndSet(0);
		if (hook != 0) {
			try {
				int ignored = (int) Native.UNHOOK_WINDOWS_HOOK_EX.invokeExact(hook);
			} catch (Throwable ignored) {
				// The hook only acts while a request is pending.
			}
		}
	}

	private static long focusedWindowOfThisProcess() throws Throwable {
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment info = arena.allocate(GUI_THREAD_INFO_SIZE, 8);
			info.set(ValueLayout.JAVA_INT, 0, GUI_THREAD_INFO_SIZE);
			if ((int) Native.GET_GUI_THREAD_INFO.invokeExact(0, info) == 0)
				return 0;
			long focus = info.get(ValueLayout.JAVA_LONG, GUI_THREAD_INFO_FOCUS_OFFSET);
			if (focus == 0)
				return 0;
			MemorySegment process = arena.allocate(ValueLayout.JAVA_INT);
			int ignored = (int) Native.GET_WINDOW_THREAD_PROCESS_ID.invokeExact(focus, process);
			return process.get(ValueLayout.JAVA_INT, 0) == (int) ProcessHandle.current().pid() ? focus : 0;
		}
	}

	/** WH_GETMESSAGE callback on the AWT toolkit thread; an escaping throwable would abort the VM. */
	@SuppressWarnings("unused")
	private static long onMessage(int code, long removeFlag, long messageAddress) {
		Request request = pending.get();
		try {
			if (request != null && code >= 0 && removeFlag == PM_REMOVE) {
				MemorySegment message = MemorySegment.ofAddress(messageAddress).reinterpret(MSG_SIZE);
				if (message.get(ValueLayout.JAVA_INT, MSG_MESSAGE_OFFSET) == Native.REQUEST_MESSAGE) {
					message.set(ValueLayout.JAVA_INT, MSG_MESSAGE_OFFSET, WM_NULL);
					boolean started = startOnToolkitThread(request, message.get(ValueLayout.JAVA_LONG, 0));
					unhook();
					SwingUtilities.invokeLater(() -> finish(request, !started));
				}
			}
		} catch (Throwable failure) {
			SwingUtilities.invokeLater(() -> finish(request, true));
		}
		try {
			return (long) Native.CALL_NEXT_HOOK_EX.invokeExact(0L, code, removeFlag, messageAddress);
		} catch (Throwable failure) {
			return 0;
		}
	}

	private static boolean startOnToolkitThread(Request request, long window) throws Throwable {
		long context = (long) Native.IMM_GET_CONTEXT.invokeExact(window);
		if (context == 0)
			return false;
		try (Arena arena = Arena.ofConfined()) {
			boolean wasOpen = (int) Native.IMM_GET_OPEN_STATUS.invokeExact(context) != 0;
			int ignored;
			if (!wasOpen)
				ignored = (int) Native.IMM_SET_OPEN_STATUS.invokeExact(context, 1);
			byte[] utf16 = request.text().getBytes(StandardCharsets.UTF_16LE);
			int size = RECONVERT_HEADER_SIZE + utf16.length + Character.BYTES;
			MemorySegment reconvert = arena.allocate(size, 8);
			int length = request.end() - request.start();
			int offset = request.start() * Character.BYTES;
			int[] header = { size, 0, request.text().length(), RECONVERT_HEADER_SIZE, length, offset, length, offset };
			MemorySegment.copy(header, 0, reconvert, ValueLayout.JAVA_INT, 0, header.length);
			MemorySegment.copy(utf16, 0, reconvert, ValueLayout.JAVA_BYTE, RECONVERT_HEADER_SIZE, utf16.length);
			boolean started = (int) Native.IMM_SET_COMPOSITION_STRING.invokeExact(context, SCS_QUERYRECONVERTSTRING,
				reconvert, size, MemorySegment.NULL, 0) != 0;
			if (started) {
				// The composition replaces the editor's selection, so it must be exactly
				// the range the IME settled on before the IME starts composing.
				int composedStart = reconvert.getAtIndex(ValueLayout.JAVA_INT, 5) / Character.BYTES;
				int composedEnd = composedStart + reconvert.getAtIndex(ValueLayout.JAVA_INT, 4);
				SwingUtilities.invokeLater(() -> {
					if (request.text().equals(request.editor().getText()))
						request.editor().select(composedStart, composedEnd);
				});
				started = (int) Native.IMM_SET_COMPOSITION_STRING.invokeExact(context, SCS_SETRECONVERTSTRING,
					reconvert, size, MemorySegment.NULL, 0) != 0;
			}
			if (!started && !wasOpen)
				ignored = (int) Native.IMM_SET_OPEN_STATUS.invokeExact(context, 0);
			return started;
		} finally {
			int ignored = (int) Native.IMM_RELEASE_CONTEXT.invokeExact(window, context);
		}
	}

	/**
	 * Microsoft IME commits the original text when reconversion is cancelled, but
	 * the composition has already replaced the selection, so an IME that just ends
	 * the composition would otherwise lose the cell's text.
	 */
	static final class CancelGuard implements InputMethodListener {
		private final JTextComponent editor;
		private final String text;
		private final int start;
		private final int end;
		private boolean composing;

		CancelGuard(JTextComponent editor, String text, int start, int end) {
			this.editor = editor;
			this.text = text;
			this.start = start;
			this.end = end;
		}

		@Override
		public void inputMethodTextChanged(InputMethodEvent event) {
			if (event.getCommittedCharacterCount() > 0) {
				editor.removeInputMethodListener(this);
			} else if (ImeTextInputSupport.isComposing(event)) {
				composing = true;
			} else if (composing) {
				editor.removeInputMethodListener(this);
				// Listeners run before the editor applies the event.
				SwingUtilities.invokeLater(() -> {
					editor.setText(text);
					editor.select(start, end);
				});
			}
		}

		@Override
		public void caretPositionChanged(InputMethodEvent event) {
		}
	}

	/** Bound on first use, so other platforms and unsupported runtimes never touch these libraries. */
	private static final class Native {
		private static final Linker LINKER = Linker.nativeLinker();
		private static final SymbolLookup USER32 = SymbolLookup.libraryLookup("user32", Arena.global());
		private static final SymbolLookup IMM32 = SymbolLookup.libraryLookup("imm32", Arena.global());
		private static final ValueLayout.OfInt INT = ValueLayout.JAVA_INT;
		private static final ValueLayout.OfLong HANDLE = ValueLayout.JAVA_LONG;
		private static final ValueLayout POINTER = ValueLayout.ADDRESS;

		static final MethodHandle GET_GUI_THREAD_INFO = user32("GetGUIThreadInfo", FunctionDescriptor.of(INT, INT, POINTER));
		static final MethodHandle GET_WINDOW_THREAD_PROCESS_ID = user32("GetWindowThreadProcessId", FunctionDescriptor.of(INT, HANDLE, POINTER));
		static final MethodHandle GET_KEYBOARD_LAYOUT = user32("GetKeyboardLayout", FunctionDescriptor.of(HANDLE, INT));
		static final MethodHandle SET_WINDOWS_HOOK_EX = user32("SetWindowsHookExW", FunctionDescriptor.of(HANDLE, INT, POINTER, HANDLE, INT));
		static final MethodHandle CALL_NEXT_HOOK_EX = user32("CallNextHookEx", FunctionDescriptor.of(HANDLE, HANDLE, INT, HANDLE, HANDLE));
		static final MethodHandle UNHOOK_WINDOWS_HOOK_EX = user32("UnhookWindowsHookEx", FunctionDescriptor.of(INT, HANDLE));
		static final MethodHandle POST_MESSAGE = user32("PostMessageW", FunctionDescriptor.of(INT, HANDLE, INT, HANDLE, HANDLE));
		static final MethodHandle IMM_IS_IME = imm32("ImmIsIME", FunctionDescriptor.of(INT, HANDLE));
		static final MethodHandle IMM_GET_CONTEXT = imm32("ImmGetContext", FunctionDescriptor.of(HANDLE, HANDLE));
		static final MethodHandle IMM_RELEASE_CONTEXT = imm32("ImmReleaseContext", FunctionDescriptor.of(INT, HANDLE, HANDLE));
		static final MethodHandle IMM_GET_OPEN_STATUS = imm32("ImmGetOpenStatus", FunctionDescriptor.of(INT, HANDLE));
		static final MethodHandle IMM_SET_OPEN_STATUS = imm32("ImmSetOpenStatus", FunctionDescriptor.of(INT, HANDLE, INT));
		static final MethodHandle IMM_SET_COMPOSITION_STRING = imm32("ImmSetCompositionStringW",
			FunctionDescriptor.of(INT, HANDLE, INT, POINTER, INT, POINTER, INT));
		static final int REQUEST_MESSAGE = registerRequestMessage();
		static final MemorySegment HOOK_STUB = hookStub();

		private static MethodHandle user32(String name, FunctionDescriptor descriptor) {
			return LINKER.downcallHandle(USER32.findOrThrow(name), descriptor);
		}

		private static MethodHandle imm32(String name, FunctionDescriptor descriptor) {
			return LINKER.downcallHandle(IMM32.findOrThrow(name), descriptor);
		}

		private static int registerRequestMessage() {
			try {
				MethodHandle register = user32("RegisterWindowMessageW", FunctionDescriptor.of(INT, POINTER));
				return (int) register.invokeExact(Arena.global().allocateFrom("microProject.ImeReconversion", StandardCharsets.UTF_16LE));
			} catch (Throwable failure) {
				throw new ExceptionInInitializerError(failure);
			}
		}

		private static MemorySegment hookStub() {
			try {
				MethodHandle callback = MethodHandles.lookup().findStatic(WindowsImeReconversion.class, "onMessage",
					MethodType.methodType(long.class, int.class, long.class, long.class));
				return LINKER.upcallStub(callback, FunctionDescriptor.of(HANDLE, INT, HANDLE, HANDLE), Arena.global());
			} catch (ReflectiveOperationException failure) {
				throw new ExceptionInInitializerError(failure);
			}
		}
	}
}
