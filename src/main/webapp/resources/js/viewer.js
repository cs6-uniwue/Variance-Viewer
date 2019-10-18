/***************************************
 **************** GUI ******************
 ***************************************/
let deselected = [];
let deselectedTypes = [""];
let remove = [];

let displaySwitches = document.querySelectorAll(".display-switch");
displaySwitches.forEach(function (displaySwitch) {
    const wrapper = displaySwitch.closest(".display-switch-wrapper");
    const variancetype = displaySwitch.dataset.type;

    if (deselectedTypes.indexOf(variancetype) > -1)
        deselected.push(displaySwitch);

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
let statisticSection = document.querySelector("#statistics");
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
function openStatistics() {
    statisticSection.style.display = "block";
    blur.style.display = "block";
}
function closeStatistics() {
    statisticSection.style.display = "none";
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
            a.href = window.URL.createObjectURL(new Blob([convertToXMLString(getTEIconformJSON(), "tei")], { type: "text/xml" }));
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
document.querySelector("#statistics-close").addEventListener("click", () => closeStatistics());
document.querySelector("#download-save").addEventListener("click", () => download());
document.querySelectorAll(".openDownload").forEach((open) => open.addEventListener("click", () => openDownload()));
document.querySelectorAll(".openHelp").forEach((open) => open.addEventListener("click", () => openHelp()));
document.querySelectorAll(".openStatistics").forEach((open) => open.addEventListener("click", () => openStatistics()));
blur.addEventListener("click", () => {closeDownload(); closeHelp(); closeStatistics()});


/***************************************
 ************ Converter ****************
 ***************************************/

const GROUPID = "de.uniwue.diff";

/**
 * Transform the exportJSON into a TEI conform UIMA JSON
 * (Convert de.uniwue.diff.* into TEI types and add additional annotations if neccesary)
 */
function getTEIconformJSON() {
    // Clone exportJSON
    let teiJson = JSON.parse(JSON.stringify(exportJSON));

    let newAnnotations = [];

    let directIncluding = null;
    let jsonID = teiJson.annotations.length;

    // Add IDs to every annotation without one
    teiJson.annotations.forEach(a => { 
        if((typeof value === 'undefined' || variable === null) || a.jsonId === "#undefined") a.jsonID = jsonID++;
    });

    // Change de.uniwue.diff.type.{INSERT,DELETE,CHANGE} annotations to TEI annotations
    teiJson.annotations.forEach(a => {
        switch (a.type) {
            case GROUPID + ".type.INSERT":
                a.type = "de.uniwue.kalimachos.coref.type.TeiType";
                a.features.TagName = "rdg";
                a.features.Attributes = "";
                
                const insertParent = getDiffRootOrNull(a.jsonId);

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
                    let parent = findNearestParent(app);
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
                const deleteParent = getDiffRootOrNull(a.jsonId);

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
                    let parent = findNearestParent(app);
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

                let parent = findNearestParent(a);
                if(parent){
                    a.features.Parent = {jsonId:parent};
                }
                break;
            default:
        }
    });

    // Delete unneeded UIMA types specifications 
    teiJson.annotations = teiJson.annotations.concat(newAnnotations);
    teiJson.types = teiJson.types.filter(c => !c.name.includes(GROUPID));

    // Resolve TEI references by replaceing jsonIds with their json object
    const map = new Map();
    for(const anno of teiJson.annotations) {
        map.set(anno.jsonId, anno);
    }

    for(const anno of teiJson.annotations) {
        const features = anno.features;
        for (const member in features) {
            // Replace every {jsonId:<id>} with the real object
            if (features[member].hasOwnProperty('jsonId')) {
                features[member] = map.get(features[member].jsonId);
            }
        }
    }

    return teiJson;
}

/** 
 * Find the root object of a de.uniwue.diff.* by its jsonId and return it or null if none exist.
 */
function getDiffRootOrNull(jsonId) {
    const change = GROUPID + ".type.CHANGE";
    let parent = null;

    exportJSON.annotations.forEach(a => {
        if (a.type === change && ((a.features.insert && a.features.insert.jsonId == jsonId) 
                                    || (a.features.delete && a.features.delete.jsonId == jsonId)))
            parent = a;
    });

    return parent;
}

/** 
 * Find the parent of an annotations by checking for the annotation directly surrounding it,
 * or null if none exist
 */
function findNearestParent(annotation){
    // Get all tags directly surrounding app, that are not rdg and lem, to set them as parent of app
    const directSurrounding = exportJSON.annotations.filter(a => !a.type.includes(GROUPID))
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

/**
 * Check if a document is a tei document by checking its annotations.
 * 
 * @param {*} annotations 
 */
function checkForTEIType(annotations) {
    for (let i = 0; i < annotations.length; i++) {
        if (annotations[i].type === "de.uniwue.kalimachos.coref.type.TeiType") {
            return true;
        }
    }
    return false;
}

/**
 * Convert a UIMA document to a xml string.
 * 
 * @param {*} uimaJson 
 * @param {*} format 
 */
function convertToXMLString(uimaJson, format) {
    let xmlString = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n';
    let containsTeiType = checkForTEIType(uimaJson.annotations);

    if (format === "tei") {
        if (!containsTeiType) {
            xmlString += '<TEI xmlns="http://www.tei-c.org/ns/1.0">\n';
            //check if there are teiType annotations included; and if so change these to the according TEI
            //append the TEI header
            xmlString += "<teiHeader>\n" +
                " <fileDesc>\n" +
                "  <titleStmt>\n" +
                "   <title></title>\n" +
                "   <respStmt>\n" +
                "    <resp></resp>\n" +
                "    <name></name>\n" +
                "   </respStmt>\n" +
                "  </titleStmt>\n" +
                "  <publicationStmt>\n" +
                "   <distributor></distributor>\n" +
                "  </publicationStmt>\n" +
                "  <sourceDesc>\n" +
                "   <bibl></bibl>\n" +
                "  </sourceDesc>\n" +
                " </fileDesc>\n" +
                "</teiHeader>" +
                " <text>\n" +
                "  <front>\n" +
                "<!-- front matter of copy text, if any, goes here -->\n" +
                "  </front>\n" +
                "  <body>"
        }
        else {
            //now there are tei types available which means we had a valid TEI document before!
            uimaJson.annotations = convertAnnotations(uimaJson.annotations);
        }
    }

    //sort again and also respect the parent hierarchy
    const tree = buildAnnotationTree(uimaJson.annotations);

    //now we can actually build the xml string
    xmlString += parseTree(tree,uimaJson.text);

    //if it is a TEI document we have to close it accordingly
    if (format === "tei") {
        if (!containsTeiType) {
            xmlString += "  </body>\n" +
                "  <back>\n" +
                "<!-- back matter of copy text, if any, goes here -->\n" +
                "  </back>\n" +
                " </text>\n" +
                "</TEI>"
        }
    }
    return xmlString;
}

/**
 * Annotation comparator, sorting annotations by their position in the text.
 * 
 * @param {*} anno1 
 * @param {*} anno2 
 */
const comparator = function (anno1, anno2) {
    //sort by begin
    if (anno1.begin !== anno2.begin) {
        return anno1.begin - anno2.begin;
    }

    //sort by end
    return anno1.end - anno2.end;
};

/**
 * Building an annotation tree from a list of annotation.
 * The tree starts with all annotations without a parent annotation.
 * Every annotation in the tree will point to all its children and annotations on 
 * the same level will be sorted by their position in the text.
 * 
 * @param {*} annotations 
 */
function buildAnnotationTree(annotations) {
    // Set all children as children of their parents and get all root annotations
    const rootAnnos = [];
    for(const anno of annotations){
        if(anno.type !== "uima.tcas.DocumentAnnotation"){
            if(("features" in anno) && (("parent" in anno.features) || ("Parent" in anno.features))){
                const parent = (anno.features.parent || anno.features.Parent);
                // Init children in parent if not exists
                delete anno.features.parent;
                delete anno.features.Parent;
                parent.children = (parent.children || []);
                parent.children.push(anno);
                parent.children.sort(comparator);
            } else {
                rootAnnos.push(anno);
            }
        }
    }
    rootAnnos.sort(comparator);
    return rootAnnos;
}

/**
 * Parse an annotations tree and text into an xml string.
 * 
 * @param {*} annotations 
 * @param {*} text 
 */
function parseTree(annotations, text){
    // Sort root annotations
    annotations.sort(comparator);
    let xmlString = "";
    
    if(annotations.length > 0){
        let lastEnd = annotations[0].begin;

        for(const currentAnno of annotations){
            // Add text inbetween annotations
            if(lastEnd < currentAnno.begin){
                xmlString += text.substring(lastEnd,currentAnno.begin);
            }

            // Get importent annotation information
            const currentType = currentAnno.type.split(".").pop();
            const annoFeatureString = createFeaturesAsXMLString(currentAnno);
            const children = currentAnno.children;

            // Add empty
            if (currentAnno.end === currentAnno.begin && (!children || children.length == 0)) {
                if(annoFeatureString){
                    xmlString += "<"+currentType+" "+annoFeatureString+"/>"
                } else {
                    xmlString += "<"+currentType+"/>"
                }
            } else {
                xmlString += "<"+currentType+" "+annoFeatureString+">"
                if(children && children.length > 0){
                    children.sort(comparator);

                    // Extract text before children
                    const begin_children = children[0].begin;
                    if(currentAnno.begin < begin_children){
                        xmlString += text.substring(currentAnno.begin,begin_children);
                    }

                    xmlString += parseTree(children, text);

                    // Extract text after children
                    const end_children = children[children.length-1].end;
                    if(end_children < currentAnno.end){
                        xmlString += text.substring(end_children,currentAnno.end);
                    }
                } else { 
                    xmlString += text.substring(currentAnno.begin,currentAnno.end);
                }
                xmlString += "</"+currentType+">"
            }
            lastEnd = currentAnno.end;
        }
    }
    return xmlString;
}

/**
 * Create an xml string from all features of an annotation
 * 
 * @param {*} anno 
 */
function createFeaturesAsXMLString(anno) {

    let annoString = "";

    for (let feat in anno.features) {
        //TODO filter  features that should not get serialized ( i currenlty dont know if there are any?)
        let value = anno.features[feat];
        annoString += " ";

        if (value) {
            //now if it is an array
            if (_.isArray(value)) {
                //it is either an array of primitives or an array of references
                if (value[0] && _.isObject(value[0])) {
                    //it is an array of objects
                    annoString += feat + '="';
                    for (let i = 0; i < value.length; i++) {
                        annoString += "#" + value[i].xmlId + " ";
                    }
                    annoString.trim();
                    annoString += '"';
                }
                else {
                    //it is an array of primitives, we just add those, separated by spaces
                    annoString += feat + '="';
                    for (let i = 0; i < value.length; i++) {
                        annoString += _.escape(value[i]) + " ";
                    }
                    annoString.trim();
                    annoString += '"';

                }
            }
            else {

                //now it is either an object or a primitive
                if (_.isObject(value)) {
                    //add the reference to the xmlID
                    annoString += feat + '="';
                    annoString += "#" + value.xmlId;
                    annoString += '"';

                }
                else {
                    //just a primitive
                    //add the reference to the xmlID
                    annoString += feat + '="';
                    annoString += _.escape(value);
                    annoString += '"';
                }
            }
        }

    }
    annoString = annoString.trim();

    return annoString;
}

/**
 * Convert TEI Annotations to real annotations.
 * 
 * @param {*} annotations 
 */
function convertAnnotations(annotations) {
    for (let i = 0; i < annotations.length; i++) {
        let teiAnno = annotations[i];
        //we skip the document annotation!
        if (teiAnno.type === "de.uniwue.kalimachos.coref.type.TeiType") {
            teiAnno.type = teiAnno.features.TagName.trim();
            delete teiAnno.features.TagName;
            //and all features!
            let featArr = teiAnno.features["Attributes"].split("##");
            for (let j = 0; j < featArr.length; j++) {
                let featName = featArr[j].split("=")[0].trim();
                let value = featArr[j].split("=")[1];
                if (value) {
                    value = value.trim();
                }
                if (featName && value && featName !== "null" && value !== "null"){
                    teiAnno.features[featName] = value;
                }
            }
            delete teiAnno.features["Attributes"];
        }
    }
    return annotations;
}