//*** Legend ***//
let deselected = [];
let deselectedTypes = ["SEPARATION"];
let remove = [];
let removeTypes = ["PARATEXT"];

let displaySwitches = document.querySelectorAll(".display-switch");
displaySwitches.forEach(function (displaySwitch) {
    const wrapper = displaySwitch.closest(".display-switch-wrapper");

    const variancetype = displaySwitch.dataset.type;

    if (deselectedTypes.indexOf(variancetype) > -1)
        deselected.push(displaySwitch);

    if (removeTypes.indexOf(variancetype) > -1)
        remove.push(wrapper);

    displaySwitch.addEventListener("change", function () {
        let lines = document.querySelectorAll(".line-fragment." + variancetype);

        if (this.checked) {
            lines.forEach(function (line) {
                line.classList.add("ACTIVE");
            });
            wrapper.classList.remove("INACTIVE");
        } else {
            lines.forEach(function (line) {
                line.classList.remove("ACTIVE");
            });
            wrapper.classList.add("INACTIVE");
        }
    })
});

let allDisplaySwitches = document.querySelectorAll(".display-switch-all");
allDisplaySwitches.forEach((allDS) => allDS.addEventListener("change", () =>
    displaySwitches.forEach((ds) => { ds.checked = allDS.checked; ds.dispatchEvent(new Event("change")) })));

//Deselect/remove switches
deselected.forEach((ds) => { ds.checked = false; ds.dispatchEvent(new Event("change")) });
remove.forEach((ds) => ds.style.display = "none");

/*** Download ***/
let downloadSection = document.querySelector("#download");
let blur = document.querySelector("#blur");
let fileNameSelect = document.querySelector("#download-filename");
let fileTypeSelect = document.querySelector("#download-filetype");
let browserWarning = document.querySelector("#browser-warning");

function openDownload() {
    downloadSection.style.display = "block";
    blur.style.display = "block";
}
function closeDownload() {
    downloadSection.style.display = "none";
    blur.style.display = "none";
}
function download() {
    let filetype = fileTypeSelect.value;
    let filename = fileNameSelect.value;

    let a = window.document.createElement('a');
    closeDownload();

    switch (filetype) {
        case "jsonAthen":
            a.href = window.URL.createObjectURL(new Blob([JSON.stringify(exportJSON)], { type: "text/json" }));
            a.download = filename + ".json";
            break;
        case "pdf":
            let websiteTitle = document.title;
            document.title = filename;
            window.print();
            document.title = websiteTitle;
            break;
        case "tei":
            var tei = webEditorIO.serializeDocumentToFormat(getTEIconformJSON(), "tei");
            tei = tei.replace(/ xml-id=\"([^"]*)\"/g, "");
            tei = tei.replace(/ xml:id=\"([^"]*)\"/g, "");
            tei = tei.replace(/ ref=\"([^"]*)\"/g, "");
            tei = tei.replace(/ parent=\"([^"]*)\"/g, "");
            tei = tei.replace(/ Parent=\"([^"]*)\"/g, "");
            tei = tei.replace(/ TagName=\"([^"]*)\"/g, "");
            tei = tei.replace(/ TagName=\"([^"]*)\"/g, "");
            tei = tei.replace(/<DocumentAnnotation language="x-unspecified">/g,"");
            tei = tei.replace(/<\/DocumentAnnotation>/g,"");
            tei = tei.replace(/xmlns="http:\/\/www\.tei-c\.org\/ns\/1\.0\nxmlns:rws"/g, 'xmlns="http://www.tei-c.org/ns/1.0"');
            tei = '<?xml version="1.0" encoding="UTF-8"?>\n'+tei  
            a.href = window.URL.createObjectURL(new Blob([tei], { type: "text/xml" }));
            a.download = filename + ".xml";
            break;

    }

    // Append anchor to body.
    document.body.appendChild(a);
    a.click();

    // Remove anchor from body
    document.body.removeChild(a);
}
// Event listener
fileTypeSelect.addEventListener("change", () => {
    let filetype = fileTypeSelect.value;
    let usesChromium = !!window.chrome;

    switch (filetype) {
        case "pdf":
            if (!usesChromium)
                browserWarning.style.display = "block";
            break;
        default:
            browserWarning.style.display = "none";
    }
});
document.querySelector("#download-cancel").addEventListener("click", () => closeDownload());
document.querySelector("#download-save").addEventListener("click", () => download());
document.querySelectorAll(".openDownload").forEach((open) => open.addEventListener("click", () => openDownload()));
blur.addEventListener("click", () => closeDownload());


/*** Converter ***/
const GROUPID = "de.uniwue.diff";
function getTEIconformJSON() {
    let teiJson = JSON.parse(JSON.stringify(exportJSON));

    let newAnnotations = [];

    let directIncluding = null;
    let jsonID = teiJson.annotations.length;

    teiJson.annotations.forEach(a => { if(!a.jsonId || a.jsonId === "#undefined") a.jsonID = jsonID++;});

    teiJson.annotations.forEach(a => {
        switch (a.type) {
            case GROUPID + ".type.INSERT":
                a.type = "rdg";
                const insertParent = getParentOrNull(a.jsonId);

                if (insertParent == null) {
                    const newParentId = jsonID++;
                    newAnnotations.push({ "type": "app", "jsonId": newParentId, "begin": a.begin, "end": a.end, "features": { "type": a.features["variance-type"] } });
                    newAnnotations.push({ "type": "lem", "begin": a.begin, "end": a.begin, "features": { "parent": { "jsonId": newParentId } } });
                    a.features.parent = { "jsonId": newParentId };
                } else {
                    a.features.rend = a.features["annotations"];
                    a.features.parent = { "jsonId": insertParent.jsonId };
                }
                delete a.features["variance-type"];
                delete a.features["annotations"];

                directIncluding = teiJson.annotations.filter(o => o.begin === a.begin && o.end === a.end);
                if(directIncluding.length > 0){
                    let parent = a;
                    directIncluding.forEach(s => {if(!["rdg","lem","app"].includes(s.type)) {s.features.parent = {jsonId:parent.jsonId}; parent = s}})
                }
                break;
            case GROUPID + ".type.DELETE":
                a.type = "lem";
                const deleteParent = getParentOrNull(a.jsonId);

                if (deleteParent == null) {
                    const newParentId = jsonID++;
                    newAnnotations.push({ "type": "rdg", "begin": a.end, "end": a.end, "features": { "parent": { "jsonId": newParentId } } });
                    newAnnotations.push({ "type": "app", "jsonId": newParentId, "begin": a.begin, "end": a.end, "features": { "type": a.features["variance-type"] } });
                    a.features.parent = { "jsonId": newParentId };
                } else {
                    a.features.rend = a.features["annotations"];
                    a.features.parent = { "jsonId": deleteParent.jsonId };
                }
                delete a.features["variance-type"];
                delete a.features["annotations"];

                directIncluding = teiJson.annotations.filter(o => o.begin === a.begin && o.end === a.end);
                if(directIncluding.length > 0){
                    let parent = a;
                    directIncluding.forEach(s => {if(!["rdg","lem","app"].includes(s.type)) {s.features.parent = {jsonId:parent.jsonId}; parent = s}})
                }
                break;
            case GROUPID + ".type.CHANGE":
                a.type = "app";
                delete a.features["insert"];
                delete a.features["delete"];

                const directSurrounding = teiJson.annotations.filter(o => o.begin === a.begin && o.end === a.end);
                if(directSurrounding.length > 0){
                    let child = a;
                    directSurrounding.forEach(s => {if(!["rdg","lem","app"].includes(s.type)){child.features.parent = {jsonId:s.jsonId}; child = s; }})
                }
                break;
            default:
        }
    });

    teiJson.annotations = teiJson.annotations.filter(a => !a.type.includes(GROUPID));

    teiJson.annotations = teiJson.annotations.concat(newAnnotations);
    const parentFeature = {name:"parent",range:"RANGE_ANNOTATION"};
    teiJson.types.forEach(t => {
        switch (t.name) {
            case GROUPID + ".type.INSERT":
                t.name = "rdg";
                t.features.forEach(f => {
                    switch (f.name) {
                        case "variance-type": f.name = "type"; break;
                        case "annotations": f.name = "rend"; break;
                    }
                });
                break;
            case GROUPID + ".type.DELETE":
                t.name = "lem";
                t.features.forEach(f => {
                    switch (f.name) {
                        case "variance-type": f.name = "type"; break;
                        case "annotations": f.name = "rend"; break;
                    }
                });
                break;
            case GROUPID + ".type.CHANGE":
                t.name = "app";
                break;
            default:
                
        }
        t.features.push(parentFeature);
    });

    resolveReferences(teiJson.annotations);

    return teiJson;
}

function getParentOrNull(jsonId) {
    const change = GROUPID + ".type.CHANGE";
    let parent = null;

    exportJSON.annotations.forEach(a => {
        if (a.type === change && ((a.features.insert && a.features.insert.jsonId == jsonId) || (a.features.delete && a.features.delete.jsonId == jsonId)))
            parent = a;
    });

    return parent;
}

function resolveReferences(annotations) {
    //map each annotation to its id
    let map = new Map();

    let len = annotations.length;
    for (let i = 0; i < len; i++) {
        map.set(annotations[i].jsonId, annotations[i]);
    }

    //and now replace the features with the according annotations
    //loop through all annotations and all features

    for (let i = 0; i < len; i++) {

        let features = annotations[i].features;

        //loop through features
        for (let member in features) {
            //if it has an id it is an object
            if (features[member].hasOwnProperty('jsonId')) {
                //replace it with the mapped annotation
                features[member] = map.get(features[member].jsonId);
            }
        }

    }
};