/**
 * Part of the WebAthen Application used for parsing from and to TEI xml
 */
const RANGE_PRIMITIVE = "RANGE_PRIMITIVE";
const RANGE_ANNOTATION = "RANGE_ANNOTATION";
const RANGE_ARRAY_PRIMITIVE = "RANGE_ARRAY_PRIMITIVE";
const RANGE_ARRAY_ANNOTATION = "RANGE_ARRAY_ANNOTATION";
const XMI_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
const FEATURE_RANGE_UNSPECIFIED = "RANGE_UNSPECIFIED";
const XMI_WEBATHEN_NAMESPACE = "de.uniwue.webathen.type.";
const XMI_VERSION = "2.0";
const XMI_NAMESPACE = "http://www.omg.org/XMI";
const ATHEN_DEFAULT_NAMESPACE = "de.uniwue.athen.";
const XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
const TEI_HEADER = "<TEI xmlns=\"http://www.tei-c.org/ns/1.0\">";

//returns a string that contains all to be able to save the document!!
function serializeDocumentToFormat(document, format, xmlMapping) {

    if (format === 'json') {
        return JSON.stringify({
            text: document.text,
            annotations: document.annotations,
            types: document.types
        });
    }
    else if (format === "txt") {
        return document.text;
    }

    else if (format === "xml" || format === "tei") {
        return convertToXMLString(document, xmlMapping, format);
    }
}

function sortAnnotations(annotations) {
    // Set all children as children of their parents and get all root annotations
    const rootAnnos = [];
    for(const anno of annotations){
        if(("features" in anno) && (("parent" in anno.features) || ("Parent" in anno.features))){
            const parent = (anno.features.parent || anno.features.Parent);
            // Init children in parent if not exists
            delete anno.features.parent;
            delete anno.features.Parent;
            parent.children = (parent.children || []);
            parent.children.push(anno);
        } else {
            rootAnnos.push(anno);
        }
    }

    return unfoldChildren(rootAnnos);
}

const comparator = function (anno1, anno2) {
    //sort by begin
    if (anno1.begin !== anno2.begin) {
        return anno1.begin - anno2.begin;
    }

    //sort by end
    if (anno1.end !== anno2.end) {
        return anno2.end - anno1.end;
    }

    //then by end, the smaller anno should be later
    return anno2.end - anno1.end;
};

function unfoldChildren(annotations){
    // Sort root annotations
    annotations.sort(comparator);
    sortedAnnos = [];
    for(const rootAnno of annotations){
        sortedAnnos.push(rootAnno);
        const children = rootAnno.children;
        delete rootAnno.children;

        if(children){
            sortedAnnos = sortedAnnos.concat(unfoldChildren(children));
        }
    }

    return sortedAnnos;
}
function convertToXMLString(editorDocument, mapping, format) {

    let xmlString = format === "xml" ? XML_HEADER : "";
    let containsTeiType = checkForTEIType(editorDocument.annotations);

    //this map stores which types can have which other types as children
    let childrenMap = {};
    if (format === "tei") {


        if (!containsTeiType) {
            xmlString += TEI_HEADER;
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
            editorDocument.annotations = convertAnnotations(editorDocument.annotations);
        }
    }

    //sort again and also respect the parent hierarchy
    annos = sortAnnotations(editorDocument.annotations);
    //now we can actually build the xml string

    let currentTextIndex = 0;
    let currentAnnoIndex = 0;
    let currentAnno = annos[currentAnnoIndex];
    const openAnnos = [];

    outer:   while (currentTextIndex < editorDocument.text.length) {

        //check if we need to close one or more annotations
        for (let j = openAnnos.length - 1; j >= 0; j--) {
            if (openAnnos[j].end === currentTextIndex) {

                if (currentAnno && currentAnno.end === currentAnno.begin) {
                    //special case for empty annotations
                    if (childrenMap[openAnnos[j].type] && childrenMap[openAnnos[j].type].indexOf(currentAnno.type) !== -1) {
                        //add an empty html
                        xmlString += "<" + currentAnno.type.split(".").pop() + "/>";
                        currentAnnoIndex += 1;
                        currentAnno = annos[currentAnnoIndex];
                        continue outer;
                    }
                }
                //we close it
                xmlString += "</" + openAnnos[j].type.split(".").pop().trim() + ">";
                openAnnos.pop();


            }
            else {
                //no other element can be closed
                break;
            }
        }

        if ((currentAnno && currentTextIndex < currentAnno.begin) || currentAnnoIndex === annos.length) {
            //just append the text
            xmlString += editorDocument.text.charAt(currentTextIndex);
        }

        //we append the annotation now
        if (currentAnno && currentTextIndex === currentAnno.begin) {
            let annoFeatureString = createFeaturesAsXMLString(currentAnno);
            xmlString += "<" + currentAnno.type.split(".").pop() + " " + annoFeatureString + ">";
            openAnnos.push(currentAnno);
            //and update the currentAnno
            currentAnnoIndex += 1;
            currentAnno = annos[currentAnnoIndex];
            //we continue because maybe there maybe more than 1 annotation with the same start index
            continue;
        }
        currentTextIndex += 1;
    }

    //and finally close open annos
    for (let j = openAnnos.length - 1; j >= 0; j--) {
        //we close it
        xmlString += "</" + openAnnos[j].type.split(".").pop() + ">";
    }

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

    //now we may also have to add a reference if this anno was split up
    if (anno.hasOwnProperty("ref")) {
        annoString += ' ref="#' + anno.ref + '"';
    }

    annoString += ' xml-id="' + anno.xmlId + '"';
    return annoString;
}


function convertAnnotations(annotations) {
    for (let i = 0; i < annotations.length; i++) {
        let teiAnno = annotations[i];
        //we skip the document annotation!
        if (teiAnno.type === "de.uniwue.kalimachos.coref.type.TeiType") {
            teiAnno.type = teiAnno.features.TagName.trim();
            //and all features!
            let featArr = teiAnno.features["Attributes"].split("##");
            for (let j = 0; j < featArr.length; j++) {
                let featName = featArr[j].split("=")[0].trim();
                let value = featArr[j].split("=")[1];
                if (value) {
                    value = value.trim();
                }
                teiAnno.features[featName] = value;
            }
            delete teiAnno.features["Attributes"];
        }
    }
    return annotations;
}



function checkForTEIType(annotations) {
    for (let i = 0; i < annotations.length; i++) {
        if (annotations[i].type === "de.uniwue.kalimachos.coref.type.TeiType") {
            return true;
        }
    }
    return false;
}

