//*** Legend ***//
const GROUPID = "de.uniwue.diff";
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
            if (deselectedTypes.indexOf(variancetype) > -1)
                deselectedTypes = deselectedTypes.splice(deselectedTypes.indexOf(variancetype),1);
        } else {
            lines.forEach(function (line) {
                line.classList.remove("ACTIVE");
            });
            wrapper.classList.add("INACTIVE");
            if (deselectedTypes.indexOf(variancetype) == -1)
                deselectedTypes.push(variancetype);
        }
        displaySwitches.forEach((ds) => { if(ds !== this && ds.dataset.type === variancetype) ds.checked = this.checked});
    })
});

let allDisplaySwitches = document.querySelectorAll(".display-switch-all");
allDisplaySwitches.forEach((allDS) => allDS.addEventListener("change", () =>{
    allDisplaySwitches.forEach((other) => {if(other !== allDS) other.checked = allDS.checked});
    displaySwitches.forEach((ds) => { 
        ds.checked = allDS.checked; 
        ds.dispatchEvent(new Event("change")); 
        const variancetype = ds.dataset.type;
        if(ds.checked)
            if (deselectedTypes.indexOf(variancetype) > -1)
                deselectedTypes = deselectedTypes.splice(deselectedTypes.indexOf(variancetype),1);
        else
            if (deselectedTypes.indexOf(variancetype) == -1)
                deselectedTypes.push(variancetype);
    });
}));

//Deselect/remove switches
deselected.forEach((ds) => { ds.checked = false; ds.dispatchEvent(new Event("change")) });
remove.forEach((ds) => ds.style.display = "none");

/*** Download ***/
let downloadSection = document.querySelector("#download");
let helpSection = document.querySelector("#help");
let blur = document.querySelector("#blur");
let fileNameSelect = document.querySelector("#download-filename");
let fileTypeSelect = document.querySelector("#download-filetype");
let browserWarning = document.querySelector("#browser-warning");
let formatWarning = document.querySelector("#format-warning");
let downloadSwitches = document.querySelector("#download-switches");

function openDownload() {
    if (!(format === "tei"))
        formatWarning.style.display = "block";
    downloadSection.style.display = "block";
    blur.style.display = "block";
}
function closeDownload() {
    downloadSection.style.display = "none";
    blur.style.display = "none";
}
function openHelp() {
    helpSection.style.display = "block";
    blur.style.display = "block";
}
function closeHelp() {
    helpSection.style.display = "none";
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
            var tei = serializeDocumentToFormat(getTEIconformJSON(), "tei");
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

    downloadSwitches.style.display = "none";
    browserWarning.style.display = "none";
    formatWarning.style.display = "none";
    switch (filetype) {
        case "pdf":
            downloadSwitches.style.display = "block";
            if (!usesChromium)
                browserWarning.style.display = "block";
            break;
        case "tei":
            if (!(format === "tei"))
                formatWarning.style.display = "block";
            break;
        default:
    }
});
document.querySelector("#download-cancel").addEventListener("click", () => closeDownload());
document.querySelector("#help-close").addEventListener("click", () => closeHelp());
document.querySelector("#download-save").addEventListener("click", () => download());
document.querySelectorAll(".openDownload").forEach((open) => open.addEventListener("click", () => openDownload()));
document.querySelectorAll(".openHelp").forEach((open) => open.addEventListener("click", () => openHelp()));
blur.addEventListener("click", () => {closeDownload(); closeHelp()});


/*** Converter ***/
function getTEIconformJSON() {
    let teiJson = JSON.parse(JSON.stringify(exportJSON));

    let newAnnotations = [];

    let directIncluding = null;
    let jsonID = teiJson.annotations.length;

    teiJson.annotations.forEach(a => { if((typeof value === 'undefined' || variable === null) || a.jsonId === "#undefined") a.jsonID = jsonID++;});

    teiJson.annotations.forEach(a => {
        switch (a.type) {
            case GROUPID + ".type.INSERT":
                a.type = "de.uniwue.kalimachos.coref.type.TeiType";
                a.features.TagName = "rdg";
                a.features.Attributes = "";
                
                const insertParent = getParentOrNull(a.jsonId);

                if (insertParent == null) {
                    const newParentId = jsonID++;
                    let app = { "type": "de.uniwue.kalimachos.coref.type.TeiType", "jsonId": newParentId, "begin": a.begin, "end": a.end,
                                            "features": {
                                                "type": a.features["variance-type"],
                                                "TagName":"app",
                                                "Attributes":""
                                            } 
                                        };
                    newAnnotations.push({ "type": "de.uniwue.kalimachos.coref.type.TeiType", "begin": a.begin, "end": a.begin, 
                                            "features": {
                                                "Parent": { "jsonId": newParentId },
                                                "TagName":"lem",
                                                "Attributes":""
                                            }
                                        });

                    // Find a parent for the new app
                    let parent = findAParent(app);
                    if(parent){
                        app.features.Parent = {jsonId:parent};
                    }
                    newAnnotations.push(app);

                    a.features.Parent = { "jsonId": newParentId };
                } else {
                    a.features.rend = a.features["annotations"];
                    a.features.Parent = { "jsonId": insertParent.jsonId };
                }
                delete a.features["variance-type"];
                delete a.features["annotations"];

                // Get all tags directly surrounding rdg and set them as child of rdg
                directIncluding = teiJson.annotations.filter(o => o.begin === a.begin && o.end === a.end);
                if(directIncluding.length > 0){
                    let parent = a;
                    directIncluding.forEach(s => {
                        if(!(s.features.TagName && ["rdg","lem","app"].includes(s.features.TagName))) {
                            s.features.Parent = {jsonId:parent.jsonId}; parent = s
                        }
                    });
                }
                break;
            case GROUPID + ".type.DELETE":
                a.type = "de.uniwue.kalimachos.coref.type.TeiType";
                a.features.TagName = "lem";
                a.features.Attributes = "";
                const deleteParent = getParentOrNull(a.jsonId);

                if (deleteParent == null) {
                    const newParentId = jsonID++;
                    newAnnotations.push({ "type": "de.uniwue.kalimachos.coref.type.TeiType", "begin": a.end, "end": a.end,
                                            "features": {
                                                "Parent": { "jsonId": newParentId }, 
                                                "TagName":"rdg",
                                                "Attributes":"" 
                                            }
                                        });
                    const app = { "type": "de.uniwue.kalimachos.coref.type.TeiType", "jsonId": newParentId, "begin": a.begin, "end": a.end,
                                            "features": {
                                                "type": a.features["variance-type"],
                                                "TagName":"app",
                                                "Attributes":""
                                            }
                                        };

                    // Find a parent for the new app
                    let parent = findAParent(app);
                    if(parent){
                        app.features.Parent = {jsonId:parent};
                    }
                    newAnnotations.push(app);

                    a.features.Parent = { "jsonId": newParentId };
                } else {
                    a.features.rend = a.features["annotations"];
                    a.features.Parent = { "jsonId": deleteParent.jsonId };
                }
                delete a.features["variance-type"];
                delete a.features["annotations"];

                // Get all tags directly surrounding rdg and set them as child of rdg
                directIncluding = teiJson.annotations.filter(o => o.begin === a.begin && o.end === a.end);
                if(directIncluding.length > 0){
                    let parent = a;
                    directIncluding.forEach(s => {
                        if(!(s.features.TagName && ["rdg","lem","app"].includes(s.features.TagName))) {
                            s.features.Parent = {jsonId:parent.jsonId}; parent = s
                        }
                    });
                }
                break;
            case GROUPID + ".type.CHANGE":
                a.type = "de.uniwue.kalimachos.coref.type.TeiType";
                a.features.TagName = "app";
                a.features.Attributes = "";
                delete a.features["insert"];
                delete a.features["delete"];
                a.features["type"] = a.features["variance-type"];
                delete a.features["variance-type"];

                let parent = findAParent(a);
                if(parent){
                    a.features.Parent = {jsonId:parent};
                }
                break;
            default:
        }
    });

    teiJson.annotations = teiJson.annotations.filter(a => !a.type.includes(GROUPID));

    teiJson.annotations = teiJson.annotations.concat(newAnnotations);
    const parentFeature = {name:"Parent",range:"RANGE_ANNOTATION"};
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
                t.features.push({name:'type',range:"RANGE_PRIMITIVE"});
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
        if (a.type === change && ((a.features.insert && a.features.insert.jsonId == jsonId) 
                                    || (a.features.delete && a.features.delete.jsonId == jsonId)))
            parent = a;
    });

    return parent;
}

function findAParent(annotation){
    const filterOut = [GROUPID+".type.CHANGE",GROUPID+".type.DELETE",GROUPID+".type.INSERT"]
    // Get all tags directly surrounding app, that are not rdg and lem, to set them as parent of app
    const directSurrounding = exportJSON.annotations.filter(a => !filterOut.includes(a.type))
                                                .filter(o => o.begin <= annotation.begin && annotation.end <= o.end);
    if(directSurrounding.length > 0){
        directSurrounding.sort((a,b) => {
            const c = b.begin - a.begin;
            return c != 0 ? c : a.end - b.end;
        });
        
        return directSurrounding[0].jsonId;
    } 
    return null;
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