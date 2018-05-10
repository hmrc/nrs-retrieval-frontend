NrsAjax = {
    http: function(vaultName, archiveId) {
        var xmlhttp = new XMLHttpRequest();
        xmlhttp.onload = function (e) {
            console.log(">>>>>> onload");
            if (xmlhttp.readyState === 4 && xmlhttp.status === 200) {
                setStatus(vaultName, archiveId, this.response)
            } else {
                console.log(">>>>>> not 200 response");
                NrsAjax.longPoll(vaultName, archiveId);
            }
        };
        xmlhttp.ontimeout = function() {
            console.log(">>>>>> timeout");
            NrsAjax.longPoll(vaultName, archiveId);
        };
        return xmlhttp;
    },
    longPoll: function(vaultName, archiveId) {
        console.log(">>>>>>>> longPoll");
        var xmlhttp = this.http(vaultName, archiveId);
        xmlhttp.open("GET", 'status/'+vaultName+'/'+archiveId);
        xmlhttp.timeout = 30000;
        xmlhttp.send();
    }
};

function checkStatus(index) {
    console.log(">>>>>> check status");
    var vaultName = vaultNameFromIndex(index);
    var archiveId = archiveIdFromIndex(index);
    NrsAjax.longPoll(vaultName, archiveId);
}

function setStatus(vaultName, archiveId, status) {

    console.log(">>>>>> set status");
    var index = toIndex(vaultName, archiveId);

    var success = document.getElementById("search-result-success-" + index);
    var failed = document.getElementById("search-result-failed-" + index);
    var inPorgress = document.getElementById("search-result-in-progress-" + index);

    switch(status) {
        case 'Complete':
            success.style.display = "block";
            failed.style.display = "none";
            inPorgress.style.display = "none";
            break;
        case 'Failed':
            success.style.display = "none";
            failed.style.display = "block";
            inPorgress.style.display = "none";
            break;
    }
}

function toIndex(vaultName, archiveId) {
    var seperator = "_key_";
    return seperator + vaultName + seperator + archiveId;
}

function vaultNameFromIndex(index) {
    var seperator = "_key_";
    var fields = index.split(seperator);
    return fields[1];
}

function archiveIdFromIndex(index) {
    var seperator = "_key_";
    var fields = index.split(seperator);
    return fields[2];
}
