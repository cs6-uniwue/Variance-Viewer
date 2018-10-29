/**
 *
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
// define a namespace in which we
const webEditorIO = (function () {

        function deserializeDocument(file, types, onSuccess, showProgress) {

            // read the document as regular text and interpret it afterwards
            const reader = new FileReader();
            reader.onload = function (fileLoadedEvent) {
                const text = fileLoadedEvent.target.result;

                if (file.name.endsWith(".txt")) {
                    let deserializedDocument = {
                        annotations: [],
                        text: text,
                        types: types
                    };
                    // annoying callback to be able to return that value
                    onSuccess(deserializedDocument, file.name);
                }

                else if (file.name.endsWith(".xmi")) {
                    let deserializedDocument = readXMI(text, types, showProgress);
                    if (deserializedDocument) {
                        onSuccess(deserializedDocument, file.name);
                    }
                }

                else if (file.name.endsWith(".json")) {
                    let deserializedDocument = readJSON(text);
                    if (deserializedDocument) {
                        onSuccess(deserializedDocument, file.name);
                    }
                }


                else if (file.name.endsWith(".xml") || file.name.endsWith(".html")) {
                    const deserializedXMLDocument = readXML(text, types);
                    if (deserializedXMLDocument) {
                        onSuccess(deserializedXMLDocument, file.name);
                    }
                }
                else {
                    console.log("Currently unsupported format - please contact god and pray for help!");
                }
            };
            reader.readAsText(file, "UTF-8");

        }

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
            else {
                console.log("Format: " + format + " is currently unsupported!!");
                return convertToXMIString(document);
            }
        }

        function convertToXMIString(editorDocument) {

            let xmiString = XMI_HEADER;
            let annotations = editorDocument.annotations;
            //this is not an easy task!

            //first step is to extract the namespace from the annotations
            let namespaces = {};
            //add the default UIMA namespaces!
            namespaces["xmi"] = {shortName: "xmi", url: XMI_NAMESPACE};
            namespaces["noNamespace"] = {
                shortName: "noNamespace",
                url: "http:///uima/noNamespace.ecore"
            };
            namespaces["cas"] = {shortName: "cas", url: "http:///uima/cas.ecore"};


            for (let i = 0; i < annotations.length; i++) {
                let fullTypeName = annotations[i].type;

                //we assign an ID for the serialization
                annotations[i].id = i;

                //convert to short
                let split = fullTypeName.split(".");
                let shortName = split[split.length - 1];
                if (split.length >= 2) {
                    shortName = split[split.length - 2];
                }
                //try to add this namespace
                addToNameSpaces(namespaces, shortName, fullTypeName, 0);
            }

            //now that all namespaces are created we can continue to create the header of the document
            xmiString += '<xmi:XMI ';
            for (const ns in namespaces) {
                xmiString += "xmlns:" + ns + "=\"" + namespaces[ns].url + "\" ";
            }
            //add version and close tag
            xmiString += "xmi:version=\"2.0\">";

            //assing IDs an map
            for (let i = 0; i < annotations.length; i++) {
                let currentAnno = annotations[i];
                let id = 1 + (i < 1337 ? i : i + 1);
                currentAnno.id = id;

            }

            //now we add the annotations
            //create a mapping from strings to types for a quick lokup
            let typeMapping = {};
            for (let i = 0; i < editorDocument.types.length; i++) {
                typeMapping[editorDocument.types[i].name] = editorDocument.types[i];
            }
            for (let i = 0; i < annotations.length; i++) {

                xmiString += convertAnnoToXMIString(annotations[i], namespaces, typeMapping);
            }

            //TODO do i need to mask the content??? i guess since the encoding is utf-8 i dont need to?
            //after the annotations we will append the main sofa
            //TODO the replacement is not a good solution, but i could not find any advice here!
            let lineArr = editorDocument.text.split("\n");
            let maskedText = lineArr.join("&#10;");

            lineArr = maskedText.split('"');
            maskedText = lineArr.join("&quot;");
            // maskedText = _.escapeHTML(maskedText);
            xmiString += '<cas:Sofa xmi:id="1337" sofaNum="1" sofaID="_InitialView" mimeType="text" sofaString="' + maskedText;
            xmiString += "\"/>";

            //and the according view with all annotations!
            xmiString += `<cas:View sofa="1337" members="`;
            for (let i = 0; i < annotations.length; i++) {
                xmiString += annotations[i].id + " ";
            }
            xmiString += "\"/></xmi:XMI>";
            return xmiString;
        }

        function convertAnnoToXMIString(anno, namespaces, typeMapping) {

            let annoString = "<";
            //first get the according namespace
            //TODO finish the conversion
            let shortName = "<UNKNOWN>";
            for (let key in namespaces) {
                let ns = namespaces[key];
                if (getTypeDomain(anno.type) === ns.fullName) {
                    shortName = ns.shortName;
                    break;
                }
            }
            annoString += shortName + ":";
            annoString += anno.type.split(".")[anno.type.split(".").length - 1] + " ";

            //add id, sofa begin and end!
            annoString += 'xmi:id="' + anno.id + '" sofa="1337" begin="' + anno.begin + '" end="' + anno.end + '" ';

            //then we add all regular features
            //skip those features
            let accordingType = typeMapping[anno.type];
            // console.log(accordingType);
            for (let j = 0; j < accordingType.features.length; j++) {
                let featObj = accordingType.features[j];
                if (featObj.name === "begin" || featObj.name === "end" || featObj.name === "sofa")
                    continue;
                let featName = featObj.name;
                //and now, depending on the feature type
                if (anno.features[featObj.name]) {
                    //depending on the feature type, we write into the document
                    if (featObj.range === RANGE_PRIMITIVE) {
                        annoString += featName + '="' + escapeHTML(anno.features[featName]) + '" ';
                    }

                    else if (featObj.range === RANGE_ANNOTATION) {
                        if (anno.features[featName] && anno.features[featName].id) {
                            annoString += featName + '="' + anno.features[featName].id + '" ';
                        }
                    }

                    else if (featObj.range === RANGE_ARRAY_ANNOTATION) {
                        annoString += featName + '="'
                        let annoArr = anno.features[featName];
                        for (let k = 0; k < annoArr.length; k++) {
                            if (annoArr[k] && annoArr[k].id) {
                                annoString += annoArr[k].id + " ";
                            }
                        }
                        annoString += '" ';
                    }

                    else if (featObj.range === RANGE_ARRAY_PRIMITIVE) {
                        //TODO this is the hardest one!
                        console.log("Writer does not support arrays of primitves yet due to extreme laziness!!" +
                            " One could say that writing this comment took longer than the actual implementation of the feature");
                    }
                    else {
                        console.log("Unknwon range: " + accordingType.features[featName]);
                    }

                }
            }


            //end the annotation
            annoString += "/>";

            return annoString;
        }

        function escapeHTML(string) {

            let maskedString = _.escape(string);
            let pre = document.createElement('pre');
            let text = document.createTextNode(maskedString);
            pre.appendChild(text);
            return pre.innerHTML;
            // return maskedString;
        }

        function addToNameSpaces(namespaces, value, fullTypeName, amountOfTries) {

            //update value
            let updatedValue = value;
            if (amountOfTries !== 0) {
                updatedValue += amountOfTries;
            }

            if (namespaces[updatedValue]) {
                //we need to check the fullname until the last .

                if (namespaces[updatedValue].fullName === getTypeDomain(fullTypeName)) {
                    //no problem
                    return;
                }
                else {
                    amountOfTries += 1;
                    //recursive tries
                    addToNameSpaces(namespaces, value, fullTypeName, amountOfTries);
                }
            }
            else {
                //simply add it
                let typeDomain = getTypeDomain(fullTypeName);
                let fullTypeNameDotted = typeDomain.split(".").join("/");
                let url = "http:///" + fullTypeNameDotted + ".ecore";
                namespaces[updatedValue] = {
                    shortName: updatedValue,
                    fullName: typeDomain,
                    url: url
                };
            }
        }

        function getTypeDomain(typeName) {
            let split = typeName.split(".");
            split.pop();
            let typeDomain = split.join(".");
            return typeDomain;
        }

        function sortAnnotations(annotations, childrenMap) {
            const comparator = function (anno1, anno2) {
                //sort by begin
                if (anno1.begin !== anno2.begin) {
                    return anno1.begin - anno2.begin;
                }

                //special case
                if ((anno1.begin === anno1.end || anno2.begin === anno2.end)) {
                    if (childrenMap) {
                        //same size here!!
                        if (childrenMap[anno1.type]) {
                            if (childrenMap[anno1.type].indexOf(anno2.type) !== -1) {
                                //we can have anno2 as child
                                return -1;
                            }
                        }

                        if (childrenMap[anno2.type]) {
                            if (childrenMap[anno2.type].indexOf(anno1.type) !== -1) {
                                //we can have anno2 as child
                                return 1;
                            }
                        }
                    }

                    if (anno1.begin === anno1.end)
                        return -1;

                    if (anno2.begin === anno2.end)
                        return 1;
                }

                //sort by end
                if (anno1.end !== anno2.end) {
                    return anno2.end - anno1.end;
                }

                //sort by child
                if (childrenMap) {
                    //same size here!!
                    if (childrenMap[anno1.type]) {
                        if (childrenMap[anno1.type].indexOf(anno2.type) !== -1) {
                            //we can have anno2 as child
                            return -1;
                        }
                    }

                    if (childrenMap[anno2.type]) {
                        if (childrenMap[anno2.type].indexOf(anno1.type) !== -1) {
                            //we can have anno2 as child
                            return 1;
                        }
                    }
                }

                //then by end, the smaller anno should be later
                return anno2.end - anno1.end;
            };
            annotations.sort(comparator);
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
                    editorDocument.annotations = convertAnnotations(editorDocument.annotations, childrenMap);
                }
            }

            let annos = [];
            for (let i = 0; i < editorDocument.annotations.length; i++) {
                annos[i] = editorDocument.annotations[i];
            }
            //sort them by begin and then length
            sortAnnotations(annos, childrenMap);

            //also assign ids to the annotations that will be used for
            //TODO i should probably delete xmlIDs afterwards of those annotations
            // because this currenlty changed the annos! also fill childrenmap if an annotation can contain another one
            for (let i = 0; i < annos.length; i++) {
                annos[i].xmlId = i;
                let currAnno = annos[i];
                if (currAnno.features.hasOwnProperty("parent") || currAnno.features.hasOwnProperty("Parent")) {
                    let parentType = currAnno.features.hasOwnProperty("parent") ? currAnno.features.parent.type : currAnno.features.Parent.type;
                    if (parentType && childrenMap.hasOwnProperty(parentType) && childrenMap[parentType].indexOf(currAnno.type) === -1) {
                        childrenMap[parentType].push(currAnno.type);
                    }
                    else {
                        let arr = [];
                        arr.push(currAnno.type);
                        childrenMap[parentType] = arr;
                    }
                }
            }

            //TODO respect the mapping
            //fix the oberlapping hierarchy problem
            console.log("Fix overlapping hierarchy elements");
            //TODO filter all annotation withou children to speed this up by alot!
            annos = fixOverlappingHierarchy(annos);

            //sort again and also respect the parent hierarchy
            sortAnnotations(annos, childrenMap);
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

        function fixOverlappingHierarchy(annotations, startIndex) {
            //this function splits an annotation into 2 if it suffers from overlapping hierarchy

            let index = startIndex || 0;
            outer: for (let i = index; i < annotations.length; i++) {
                let current = annotations[i];
                //current only suffers from overlapping heirarchy if there
                // is a previous annotation with smaller begin that also has a smaller end!
                for (let j = i - 1; j > 0; j--) {
                    let prev = annotations[j];

                    if (prev.begin < current.begin) {
                        //we can stop to look backwards now since this is enough
                        //check if there is a problem with overlapping hierarchies
                        if (prev.end < current.end && current.begin < prev.end) {
                            //now there is a problem
                            // console.log("Overlapping hierarchy!!!" + prev.type + "||" + current.type);
                            //we split current into 2 annotations
                            let splitBeg = prev.end;
                            let splitEnd = current.end;

                            //set the end of the first annotation
                            let shiftedAnno = {
                                begin: current.begin,
                                end: splitBeg,
                                features: prev.features,
                                type: current.type,
                                xmlId: current.xmlId,
                            }

                            let newAnno = {
                                begin: splitBeg,
                                end: splitEnd,
                                type: current.type,
                                ref: current.xmlId,
                                xmlId: current.xmlId + "_2",

                            }
                            //and obiously delete current
                            annotations.splice(i, 1);
                            annotations.push(shiftedAnno);
                            annotations.push(newAnno);


                            //now sort those annotations and call recursively
                            sortAnnotations(annotations);
                            fixOverlappingHierarchy(annotations, i);

                            break outer;
                        }
                    }
                }
            }
            return annotations;
        }


        function convertAnnotations(annotations, childrenMap) {
            for (let i = 0; i < annotations.length; i++) {
                let curr = annotations[i];
                //we skip the document annotation!
                if (curr.type === "de.uniwue.kalimachos.coref.type.TeiType") {
                    expandTEIAnno(curr, childrenMap);
                }
            }
            return annotations;
        }

        function expandTEIAnno(teiAnno, childrenMap) {

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

            if (teiAnno.features.hasOwnProperty("parent") || teiAnno.features.hasOwnProperty("Parent")) {
                let parentType = teiAnno.features.parent ? teiAnno.features.parent.features.TagName.trim() : teiAnno.features.Parent.features.TagName.trim();
                if (parentType && childrenMap.hasOwnProperty(parentType)) {
                    childrenMap[parentType].push(teiAnno.features.TagName.trim());
                }
                else if (parentType) {
                    let arr = [];
                    arr.push(teiAnno.features.TagName.trim());
                    childrenMap[parentType] = arr;
                }
            }
        }

        function readXML(xmlString, types) {

            const stack = [];
            let charOffset = 0;
            let text = "";
            let isOpeningTag = false;
            let currentOpenElement = "";
            const annotations = [];
            let insideElement = false;

            for (let i = 0; i < xmlString.length; i++) {
                const currentChar = xmlString.charAt(i);

                //if it is a opening indicator
                if (currentChar === '<') {
                    //if the next char is a / ==> this is a ending tag
                    isOpeningTag = !(xmlString.charAt(i + 1) && xmlString.charAt(i + 1) === '/');
                    insideElement = true;
                }
                //a closing char for a html/xml indicator
                else if (currentChar === '>') {

                    //the xml element has been fully read, we can now create an annotation from it

                    //empty element, we avoid the stack
                    if (xmlString.charAt(i - 1) && xmlString.charAt(i - 1) === '/') {
                        var anno = parseAnnotationFromXML(currentOpenElement);
                        anno.begin = charOffset;
                        anno.end = charOffset + 1;
                        if (stack[stack.length - 1]) {
                            anno.features.parentXML = stack[stack.length - 1].type;
                        }

                        annotations.push(anno);
                    }

                    //we put it on the stack
                    if (isOpeningTag) {
                        var anno = parseAnnotationFromXML(currentOpenElement);
                        anno.begin = charOffset;
                        //also set the parent element for easy recosntruction later on
                        if (stack[stack.length - 1]) {
                            anno.features.parentXML = stack[stack.length - 1].type;
                        }
                        stack.push(anno);
                    }
                    //a closing tag, we have to get the last element from the stack
                    else {
                        var anno = stack.pop();
                        //set the end index
                        anno.end = charOffset + 1;
                        //and save the annotation
                        annotations.push(anno);
                    }
                    insideElement = false;
                    //reset the element variable
                    currentOpenElement = "";
                }
                else if (insideElement) {
                    currentOpenElement += currentChar;
                }

                else {
                    //we count the offset
                    charOffset++;
                    //and the text
                    text += currentChar;
                }

            }
            //update the types, that is either add new types to the typesystem or add new features if they occur
            updateTypesFromAnnotations(annotations, types);

            return {
                text: text, annotations: annotations, types: types
            };
        }

        function checkForTEIType(annotations) {
            for (let i = 0; i < annotations.length; i++) {
                if (annotations[i].type === "de.uniwue.kalimachos.coref.type.TeiType") {
                    return true;
                }
            }
            return false;
        }

//update the typesystem
        function updateTypesFromAnnotations(annotations, types) {

            if (!types) {
                types = [];
            }

            for (let i = 0; i < annotations.length; i++) {
                const currentAnno = annotations[i];

                //access the current type

                //TODO function was deleted
                const bestType = getBestMatchingType(currentAnno.type, currentAnno.features, types);

                if (bestType) {
                    //loop through the features of the annotation
                    for (let key in currentAnno.features) {

                        //if the type does not have that feature yet!!
                        let hasFeature = false;
                        for (let j = 0; j < bestType.features.length; j++) {
                            if (bestType.features[j].name === key) {
                                hasFeature = true;
                                break;
                            }
                        }
                        //we can add that feature
                        if (!hasFeature) {
                            bestType.features.push({name: key, range: "RANGE_PRIMITIVE"});
                        }
                    }
                }
                else {
                    const newType = {name: currentAnno.type, features: []};
                    //we have no type, therefore we can add features to it
                    //loop through the features
                    for (let key in currentAnno.features) {

                        //we can add that feature
                        newType.features.push({name: key, range: "RANGE_PRIMITIVE"});
                    }
                    types.push(newType);
                }
            }
        }

//expects to get sth like "book att1="jkljlkj" att2="jkljl""
//TODO if the value of an attribute has spaces this will fail!!
        function parseAnnotationFromXML(elementDesc) {
            //split the element at a space
            const split = elementDesc.split(" ");

            const createdAnno = {type: ATHEN_DEFAULT_NAMESPACE + split[0], features: {}};

            //now read the features
            for (let i = 1; i < split.length; i++) {
                const att = split[i];
                //key is before =
                const key = att.split("=")[0];
                //value after, we have to remove " first
                const value = att.split("=")[1].split("\"").join("");
                createdAnno.features[key] = value;
            }
            return createdAnno;
        }


        function readJSON(jsonString) {

            const obj = JSON.parse(jsonString);
            if (obj.text) {
                if (!obj.annotations) {
                    obj.annotations = [];
                }
                if (!obj.types) {
                    obj.types = inferJSONTypesFromAnnotations(obj.annotations);
                }

                return obj;
            }
            else {
                return null;
            }
        }

//TODO this is unfinished and is required for the JSON reading
        function inferJSONTypesFromAnnotations(annotations) {

            //if there are no annotations we just return an empty list
            if (!annotations)
                return [];

            //else infer the typesystem from the annotations
            //TODO

            //maps typenames to a feature object
            const inferredTypeMap = new Map();

            const len = annotations.length;

            for (let i = 0; i < len; i++) {

                const annoType = annotations[i].type;

                const featureObj = inferredTypeMap.get(annoType);
                if (featureObj) {
                    //we append if not present

                    //for each feature of that annotation
                    for (let featAnno in annotations[i].features) {
                        if (!featureObj[featAnno]) {
                            featureObj[featAnno] = inferFeatureType(annotations[i].features[featAnno]);
                        }
                    }
                }
            }
            return [];
        }


        function inferFeatureType(featureValue) {

            //TODO finish this
            if (Array.isArray(featureValue)) {
                //check if the element at 0 position (if available if an object
            }
            return
        }


        /**
         * @param text,
         *            the whole xmi as string
         * @param types,
         *            the typesystem, this document is interpreted with
         * @return a document tripel (text,annotations,types) with the structure
         *         required for the editor
         */
        function readXMI(text, types, showProgress) {

            if (showProgress) {

                //TODO idk why my attempts did not work here...

            }
            // parse the xml
            console.time("parseXMI");
            console.time("domparse");
            parser = new DOMParser();
            xmiDoc = parser.parseFromString(text, "text/xml");
            console.timeEnd("domparse");


            //TODO also create a typesystem if none is present!
            // var docText = xmiDoc.getElementsByTagName("cas:Sofa")[0]
            // .getAttribute('sofaString');

            // and the annotations
            const elements = xmiDoc.getElementsByTagName("*");
            var text = "";
            let annotations = [];
            //the array of namespaces which will be coded into annotations
            let namespaces = {};
            //we need to keep track of the ids of annotations that are currently stored in the initial casview
            const idsToKeep = [];
            //parse all views and discard what is not needed!
            const views = {};
            let initialViewID = undefined;
            let i = 0;
            const j = elements.length;
            //those map cache intermediate results!
            let typeMap = {};
            let typeFeatureMap = {};
            for (; i < j; i++) {

                const currentElement = elements[i];
                //parse all namespaces, we need those if annotations of new types are baout to be added!
                if (currentElement.nodeName === "xmi:XMI") {
                    namespaces = parseXMINamespaces(currentElement);
                }
                else if (currentElement.nodeName.startsWith("cas:")) {
                    if (currentElement.nodeName === "cas:NULL") {
                        //idk what this is ...we ignore it simply for now, might even be a UIMA bug
                    }

                    if (currentElement.nodeName === "cas:Sofa") {
                        //parse if it is the initial view
                        if (currentElement.getAttribute("sofaID") !== "_InitialView") {
                            continue;
                        }
                        if (currentElement.getAttribute("mimeType") !== "text") {
                            console.log("Can only parse XMI with mimeType=text");
                            continue;
                        }
                        //here we have the initialview and we know that it contains text!

                        //parse the text and the sofa ID
                        text = currentElement.getAttribute("sofaString");
                        initialViewID = currentElement.getAttribute("xmi:id");


                    }

                    if (currentElement.nodeName === "cas:View") {
                        //this is the index of annotations of this cas
                        //parse this view
                        const accordingSofaID = currentElement.getAttribute("sofa");
                        let annotationsInView = currentElement.getAttribute("members").split(" ");
                        //convert to number
                        annotationsInView = annotationsInView.map(Number);
                        views[accordingSofaID] = annotationsInView;

                    }

                }
                //i assume all what comes here is an annotation!
                else {
                    //now it is either an annotation or an entry in a featurearray!
                    if (!currentElement.getAttribute("xmi:id")) {
                        //in this case this element is a feature of the parent element
                        let lastAnno = annotations[annotations.length - 1];
                        //check if assigned and add to the array
                        let featureArray = lastAnno.features[currentElement.nodeName] || [];
                        featureArray.push(currentElement.textContent);
                        lastAnno.features[currentElement.nodeName] = featureArray;


                        //check if the type does already know that feature! TODO this is kinda annoying
                        //otherwise assign it PRIMITIVE_ARRAY
                    }
                    else {
                        annotations.push(parseAnnoFromXMI(currentElement, types, namespaces, typeMap, typeFeatureMap));
                    }
                }

            }

            //now we have all elements parsed, now we still have lots of dirty work.
            //at first we need to select all annotations we want to keep, basically a filter step
            if (!initialViewID) {
                //we have to discard all annotations
                annotations = [];
            }
            else {
                //TODO this is required if you want to parse multiple cas views


                const realView = views[initialViewID];

                if (realView) {
                    let annoIndexArr = [];
                    //insert to number array for quick lookup
                    for (let i = 0; i < realView.length; i++) {
                        annoIndexArr[Number(realView[i])] = true;
                    }
                    let filteredAnnos = [];
                    for (let i = 0; i < annotations.length; i++) {
                        if (annoIndexArr[annotations[i].id]) {
                            filteredAnnos.push(annotations[i]);
                        }
                    }
                    annotations = filteredAnnos;
                }
                //     annotations = annotations.filter(function (anno) {
                //         return (realView.indexOf(anno.id) !== -1);
                //     });
                // }
                // else {
                //     annotations = [];
                // }

            }

            //next is to infer all missing feature types
            inferUnspecifiedFeatureTypes(annotations, types);


            //and at last we need to set the according objects if there were features set as objects!
            injectObjects(annotations, types);

            if (showProgress) {
                //clean up a dialog TODO
            }
            console.timeEnd("parseXMI");
            return {
                text: text,
                annotations: annotations,
                types: types
            };

        }

// this infers whether a feature is a primitive a reference to another annotation or an array of that
        function inferUnspecifiedFeatureTypes(annotations, types) {

            //at first we map annotations to types
            const annoMap = {};
            for (let i = 0; i < annotations.length; i++) {
                let annoArr = annoMap[annotations[i].type] || [];
                annoArr.push(annotations[i]);
                annoMap[annotations[i].type] = annoArr;
            }

            //and to their IDS
            //at first we map annotations
            const annoIDMap = {};
            for (let i = 0; i < annotations.length; i++) {
                annoIDMap[annotations[i].id] = annotations[i];
            }

            //loop through the typesystem and search for unknown values
            for (let i = 0; i < types.length; i++) {
                const currType = types[i];
                //loop through all features and check if there is an unknown feature
                for (let j = 0; j < currType.features.length; j++) {
                    if (currType.features[j].range === FEATURE_RANGE_UNSPECIFIED) {
                        //infer that feature
                        let inferredRange = inferFeatureRangeFromAnnotations(currType.features[j], annoMap[currType.name], annoIDMap);
                        currType.features[j].range = inferredRange;
                        console.log("Inferred FeatureType: " + inferredRange + " of feature: " + currType.features[j].name);
                    }
                }
            }
        }

//either a primitive (default) or a reference to another annotation or a reference to an array
        function inferFeatureRangeFromAnnotations(feature, annosOfType, annoIDMap) {

            let canBeAnnoArray = true;
            for (let i = 0; i < annosOfType.length; i++) {
                let feat = annosOfType[i].features[feature.name];

                if (annosOfType[i].features.hasOwnProperty(feature.name)) {

                    //first test is if it contains any blanks
                    if (feat.indexOf(" ") !== -1) {
                        //we know that it can never be an annotation
                        //either an array of annos or a primitve
                        let split = feat.split(" ");
                        for (let k = 0; k < split.length; k++) {
                            if (Number.isInteger(split[k])) {
                                if (!annoIDMap.hasOwnProperty(split[k])) {
                                    //cant be an array either => primitive
                                    return RANGE_PRIMITIVE;
                                }
                            }
                        }
                    }
                    else {
                        //now there are no whitespaces in the featurevalue which means we can only do one more thing

                        //try to parse as int
                        if (Number.isInteger(feat)) {
                            //we need to check if there is an annotation wiht this value
                            if (!annoIDMap.hasOwnProperty(feat)) {
                                //can only be a primitive
                                return RANGE_PRIMITIVE;
                            }
                        }
                        else {
                            //guaranteed a primitive
                            return RANGE_PRIMITIVE;
                        }
                    }
                }
            }

            if (canBeAnnoArray)
                return RANGE_ARRAY_ANNOTATION;

            return RANGE_PRIMITIVE;
        }

//this assumes that all feature ranges have been inferred!
        function injectObjects(annotations, types) {
            //at first we map annotations
            const annoMap = {};
            for (let i = 0; i < annotations.length; i++) {
                annoMap[annotations[i].id] = annotations[i];
            }
            //then we loop through all annotations
            for (let i = 0; i < annotations.length; i++) {
                const anno = annotations[i];

                //get the according type
                let accordingType = null;
                for (let j = 0; j < types.length; j++) {
                    if (types[j].name === anno.type) {
                        accordingType = types[j];
                        break;
                    }
                }
                //loop through the features
                for (let j = 0; j < accordingType.features.length; j++) {
                    const feat = accordingType.features[j];
                    if (feat.range === RANGE_ANNOTATION) {
                        //access the according object if available
                        if (anno.features.hasOwnProperty(feat.name)) {
                            let val = anno.features[feat.name];
                            //set the according annotation
                            let accordingAnno = annoMap[val];
                            if (accordingAnno) {
                                anno.features[feat.name] = accordingAnno;
                            }
                        }
                    }

                    else if (feat.range === RANGE_ARRAY_ANNOTATION) {
                        //access the according object if available
                        if (anno.features.hasOwnProperty(feat.name)) {
                            let val = anno.features[feat.name];

                            //split and set
                            const annoIDs = val.split(" ");
                            //set the according annotation
                            const featureArr = [];
                            for (let k = 0; k < annoIDs.length; k++) {
                                let accordingAnno = annoMap[annoIDs[k]];
                                if (accordingAnno) {
                                    featureArr[k] = accordingAnno;
                                }
                            }
                            anno.features[feat.name] = featureArr;

                        }
                    }


                }
            }
        }

        function parseXMINamespaces(element) {
            const attList = element.attributes;
            const namespaces = {};
            for (let i = 0; i < attList.length; i++) {
                const attName = attList[i].nodeName;
                if (attName.startsWith("xmlns")) {
                    //this means it is a namespace attribute!
                    const nameSpaceId = attName.split(":")[1];
                    const namespaceValue = attList[i].nodeValue;

                    //get the usual dotted version of the namespace
                    const shortedNS = namespaceValue.substring(8, namespaceValue.lastIndexOf(".ecore")).split("/").join(".");
                    //the xmi should always be present! we dont need to extract it
                    if (nameSpaceId !== "xmi") {
                        namespaces[nameSpaceId] = {
                            id: nameSpaceId,
                            name: namespaceValue,
                            shortName: shortedNS
                        };
                    }
                }
            }
            return namespaces;
        }


        function parseAnnoFromXMI(element, types, namespaces, typeMap, typeFeatureMap) {
            types = types || [];
            const ns = element.nodeName.split(":")[0];
            let typeName = element.nodeName.split(":")[1];

            //try to access the namespace in the cas (this must exist otherwise the document mgiht be corrupted somehow!)
            let accordingNs = namespaces[ns];
            if (!accordingNs) {
                accordingNs = {id: "webathen", name: "idc", shortName: XMI_WEBATHEN_NAMESPACE};
            }
            //now we have a namespace of that annotation, get the type out of types
            typeName = accordingNs.shortName + "." + typeName;
            let accordingType = null;
            if (typeMap[typeName]) {
                accordingType = typeMap[typeName];
            }
            else {
                for (let i = 0; i < types.length; i++) {
                    if (types[i].name === typeName) {
                        accordingType = types[i];
                        typeMap[typeName] = accordingType;
                        break;
                    }
                }
            }

            if (!accordingType) {
                //we create a new type and add it!
                const newType = {name: typeName, features: []};
                types.push(newType);
                accordingType = newType;
                console.log("Created new type: " + typeName);
            }
            //now there is a type, try to parse the features!!
            const anno = {type: accordingType.name, features: {}};
            const attList = element.attributes;

            for (let i = 0; i < attList.length; i++) {
                const currentAtt = attList[i];
                if (currentAtt.nodeName === "xmi:id") {
                    anno.id = Number(currentAtt.nodeValue);
                }
                else if (currentAtt.nodeName === "begin") {
                    anno.begin = Number(currentAtt.nodeValue);
                }
                else if (currentAtt.nodeName === "end") {
                    anno.end = Number(currentAtt.nodeValue);
                }
                else {
                    //a regular feature, here we will still just parse it as a string
                    const featureName = currentAtt.nodeName;
                    if (featureName === "sofa")
                        continue;
                    anno.features[featureName] = currentAtt.nodeValue;
                    //check if there is an according feature in the type, else add it
                    let accordingFeature = null;
                    if (typeFeatureMap[accordingType.name] && typeFeatureMap[accordingType.name][featureName]) {
                        accordingFeature = typeFeatureMap[accordingType.name][featureName];
                    }
                    else {
                        for (let j = 0; j < accordingType.features.length; j++) {
                            if (accordingType.features[j].name === featureName) {
                                accordingFeature = accordingType.features[j];
                                typeFeatureMap[accordingType.name] = typeFeatureMap[accordingType.name] || {};
                                typeFeatureMap[accordingType.name][featureName] = accordingFeature;
                                break;
                            }
                        }
                    }

                    if (!accordingFeature) {
                        accordingType.features.push({
                            name: featureName,
                            range: FEATURE_RANGE_UNSPECIFIED
                        });
                        console.log("Dont know feature: " + featureName + " of type: " + accordingType.name);
                    }

                }
            }


            return anno;
        }


        return {
            deserializeDocument: deserializeDocument,
            serializeDocumentToFormat: serializeDocumentToFormat
        };
    }

)
();

