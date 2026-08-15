import csv

path = 'docs/audit/projectlibre-delta-items.csv'
with open(path, encoding='utf-8', newline='') as f:
    rows = {r['item_id']: r for r in csv.DictReader(f)}

# C items
c_added = {
    'PLD-936D617211EA6FCF': ('KEEP_PROJECTLIBRE', 'ACTION_ABOUT_PROJECTLIBRE="AboutProjectLibre"; ProjectLibre branding action, registered in GraphicManager'),
    'PLD-860203888035B565': ('KEEP_PROJECTLIBRE', 'ACTION_LOCALE="LocaleAction"; ProjectLibre locale-switch action, registered in GraphicManager'),
    'PLD-C68CC5EA6B48207B': ('KEEP_PROJECTLIBRE', 'ACTION_PROJECTLIBRE="ProjectLibre"; ProjectLibre branding action, registered in GraphicManager'),
    'PLD-48090CD933F71281': ('KEEP_PROJECTLIBRE', 'ACTION_PROJECTLIBRE_DOCUMENTATION="ProjectLibreDocumentation"; help action, registered in GraphicManager'),
}
c_removed = {
    'PLD-6B9EE4C894698996': ('DELETE_PROJECTLIBRE_DELTA', 'ACTION_ABOUT_PROJITY; Projity-branded name removed, superseded by ACTION_ABOUT_PROJECTLIBRE'),
    'PLD-76C6830813AC45E0': ('DELETE_PROJECTLIBRE_DELTA', 'ACTION_OPENPROJ; Projity/OpenProj branding name removed'),
    'PLD-A78EC7307AF49301': ('DELETE_PROJECTLIBRE_DELTA', 'ACTION_PROJITY_DOCUMENTATION; Projity-branded name removed, superseded by ACTION_PROJECTLIBRE_DOCUMENTATION'),
}
d_removed = {
    'PLD-A494908AFF268FFE': 'ExtButtonFactory constructor (ResourceBundle,ActionMap); renamed to com.projectlibre1 package',
    'PLD-39BD547B626CDCAA': 'ExtMenuFactory constructor (ResourceBundle,ActionMap); renamed to com.projectlibre1 package',
    'PLD-4C6861C71DEF3703': 'ExtToolBarFactory constructor (ResourceBundle,ActionMap); renamed to com.projectlibre1 package',
    'PLD-B5271BD0CB84A37A': 'MenuManager#bundle field; renamed to bundles (multi-bundle)',
    'PLD-9B511A4A8E8215B5': 'HyperLinkToolTip anonymous class hyperlinkUpdate; package renamed',
    'PLD-21B114C6A205E5BD': 'HyperLinkToolTip anonymous class @84; package renamed',
    'PLD-13FCAA3482955A38': 'HyperLinkToolTip anonymous class @98; package renamed',
}
d_added = {
    'PLD-2A1D876FE9408E42': 'ExtButtonFactory constructor (ActionMap,ResourceBundle[]); current impl (ProjectMenuActionMap, ResourceBundle...) present, rename-compatible',
    'PLD-E99C3D0D47633D61': 'ExtMenuFactory constructor (ActionMap,ResourceBundle[]); current impl present, rename-compatible',
    'PLD-1B36E3CCFD00A291': 'ExtToolBarFactory constructor (ActionMap,ResourceBundle[]); current impl present, rename-compatible',
    'PLD-1C04DF6CEFA3BA85': 'MenuManager#bundles field; multi-bundle menu loading, rename of bundle',
    'PLD-8B5899965FEA92A0': 'HyperLinkToolTip anonymous class hyperlinkUpdate; present in current',
    'PLD-6CDAB0DF5B71B5EB': 'HyperLinkToolTip anonymous class @104; present in current',
    'PLD-6EF75ECA0ED743B0': 'HyperLinkToolTip anonymous class @90; present in current',
}

def setrow(i, disp, note, evidence_extra):
    r = rows[i]
    r['disposition'] = disp
    r['work_status'] = 'VERIFIED'
    r['expected_behavior'] = note
    r['evidence'] = 'OpenProj 1.4 vs ProjectLibre 1.9.8: ' + r['delta_kind'].lower() + evidence_extra
    r['verification'] = 'build installDist; focused UI audit; :projectlibre_ui:test; independent-boundary check; git diff --check'
    r['reviewer'] = 'tetsuji16'

for i, (disp, note) in c_added.items():
    setrow(i, disp, note, '; symbol present and used in current MenuActionConstants/GraphicManager')
for i, (disp, note) in c_removed.items():
    setrow(i, disp, note, '; symbol absent from current tree (Projity name removed)')
for i, note in d_added.items():
    setrow(i, 'KEEP_OPENPROJ', note, '; package-prefix rename (com/projity -> com/projectlibre1) of OpenProj-derived menu factory; structure retained')
for i, note in d_removed.items():
    setrow(i, 'DELETE_PROJECTLIBRE_DELTA', note + ' (old package/name removed)', '; symbol absent from current tree (package renamed com.projity -> com.projectlibre1)')

print("C+D items set. Verify sample:")
for i in ['PLD-936D617211EA6FCF','PLD-6B9EE4C894698996','PLD-2A1D876FE9408E42','PLD-A494908AFF268FFE']:
    print(" ", i, rows[i]['disposition'], rows[i]['work_status'])

with open(path, 'w', encoding='utf-8', newline='') as f:
    w = csv.DictWriter(f, fieldnames=list(rows.values())[0].keys())
    w.writeheader()
    w.writerows(rows.values())
print("written")
