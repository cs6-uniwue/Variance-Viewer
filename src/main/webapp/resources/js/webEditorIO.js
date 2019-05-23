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

/**
 * Convert a UIMA document into different formats.
 * 
 * @param {*} editorDocument 
 * @param {*} format 
 */
function serializeDocumentToFormat(document, format) {

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
        return convertToXMLString(document, format);
    }
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
 * @param {*} editorDocument 
 * @param {*} format 
 */
function convertToXMLString(editorDocument, format) {

    let xmlString = format === "xml" ? XML_HEADER : "";
    let containsTeiType = checkForTEIType(editorDocument.annotations);

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
    const tree = buildAnnotationTree(editorDocument.annotations);

    //now we can actually build the xml string
    xmlString += parseTree(tree,editorDocument.text);

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

    //now we may also have to add a reference if this anno was split up
    if (anno.hasOwnProperty("ref")) {
        annoString += ' ref="#' + anno.ref + '"';
    }

    annoString += ' xml-id="' + anno.xmlId + '"';
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