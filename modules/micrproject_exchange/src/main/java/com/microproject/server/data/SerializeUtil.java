/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.server.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.resource.EnterpriseResource;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.session.Session;
import com.microproject.util.SafeObjectInput;

/**
 *
 */
public class SerializeUtil {
    public static final boolean ZIP=true;


    public static byte[] serializeToByteArray(Object data) throws IOException {
        try (ByteArrayOutputStream bout = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bout)) {
            out.writeObject(data);
            return bout.toByteArray();
        }
    }

    public static Object deserializeFromByteArray(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
             ObjectInputStream in = SafeObjectInput.create(bin)) {
            return in.readObject();
        }
    }

    public static SerializedDataObject serialize(DataObject data, SerializedDataObjectFactory factory) throws IOException{
        SerializedDataObject r=factory.createSerializedDataObject();
        r.setUniqueId(data.getUniqueId());
        r.setName(data.getName());
        r.setDirty(data.isDirty());

        ByteArrayOutputStream bout=new ByteArrayOutputStream();
        ObjectOutputStream out;
        if (ZIP&&((data instanceof WorkCalendar)||(data instanceof EnterpriseResource)||(data instanceof ResourceImpl))){ //other are too small to be zipped
        	ZipOutputStream zout=new ZipOutputStream(bout);
        	zout.putNextEntry(new ZipEntry("Serialized"));
        	out=new ObjectOutputStream(zout);
        } else out=new ObjectOutputStream(bout);
        out.writeObject(data);
        out.close();
        bout.close();

        r.setSerialized(bout.toByteArray());
        //System.out.println(data+" size="+r.getSerialized().length);
        return r;
    }

    public static DataObject deserialize(SerializedDataObject sdata,Session session) throws IOException,ClassNotFoundException{
    	ByteArrayInputStream bin=new ByteArrayInputStream(sdata.getSerialized());
        ObjectInputStream in;
		if (ZIP&&(sdata.getType()==DataObjectConstants.CALENDAR_TYPE||sdata.getType()==DataObjectConstants.ENTERPRISE_RESOURCE_TYPE||sdata.getType()==DataObjectConstants.RESOURCE_TYPE)){
        	ZipInputStream zin=new ZipInputStream(bin);
            zin.getNextEntry();
            in=SafeObjectInput.create(zin);
		} else in=SafeObjectInput.create(bin);
        DataObject data=(DataObject)in.readObject();
        data.setUniqueId(session==null?sdata.getUniqueId():session.getId());
		if (sdata.getName() != null)
			data.setName(sdata.getName());
        return data;
    }

}
