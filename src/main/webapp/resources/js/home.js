const warning = document.getElementById("warning");
const orig_classes = warning.className;
let displayingWarning = false;

const form = document.forms["compare-form"]
let lastSelectedSetting = false;

function validateForm(){
    if (form["file1"].value == "" || form["file2"].value == "") {
        displayWarning("Please select two files to compare.");
        return false;
    }
    if (form["settings"].value == "user" && form["settingsFile"].value == "") {
        displayWarning("Please select a settings file, when choosing user settings.");
        return false;
    }
}

function displayWarning(message,time=4000){
    if(!displayingWarning){
        displayingWarning = true;
        warning.innerHTML = message;
        warning.className += " pop-in";

        setTimeout(() => {
            warning.className = orig_classes + " pop-out";
            setTimeout(() => {
                warning.className = orig_classes;
                displayingWarning = false;
            }, 300);
        }, time);
    }
}

// Select settings listener
form["settings"].onchange = () => {
	const setting = form["settings"].value;
	lastSelectedSetting = setting != "user" ? setting : lastSelectedSetting;
}

form["settingsFile"].onclick = () => {
	document.querySelector("input[value=user]").checked = true;
}

// Tabs
const tablinks = document.querySelectorAll(".tablink");
const tabcontents = document.querySelectorAll(".tabcontent");
for(const tablink of tablinks){
	// Add listeners
	tablink.onclick = function() {
		// Clear
		for(const content of tabcontents) 
			content.classList.remove("selected");
		for(const l of tablinks) 
			l.classList.remove("selected");

		// Select
		this.classList.add("selected")
		document.querySelector(`#${this.dataset["target"]}`).classList.add("selected");
	}
}
