const warning = document.getElementById("warning");
const orig_classes = warning.className;
let displayingWarning = false;

function validateForm(){
    const f1 = document.forms["compare-form"]["file1"].value;
    const f2 = document.forms["compare-form"]["file2"].value;

    if (f1 == "" || f2 == "") {
        displayWarning("Please select two files to compare.");
        return false;
    }
}

function displayWarning(message){
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
        }, 2000);
    }
}
