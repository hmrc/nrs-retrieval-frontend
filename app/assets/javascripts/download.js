NrsAjax = {
    http: function(index, vaultName, archiveId) {
        var xmlhttp = new XMLHttpRequest();
        xmlhttp.onload = function (e) {
            if (xmlhttp.readyState === 4 && xmlhttp.status === 200) {
                setStatus(index, this.response)
            } else {
                NrsAjax.checkStatus(index, vaultName, archiveId);
            }
        };
        xmlhttp.ontimeout = function() {
            NrsAjax.checkStatus(index, vaultName, archiveId);
        };
        return xmlhttp;
    },
    checkStatus: function(index, vaultName, archiveId) {
        var xmlhttp = this.http(index, vaultName, archiveId);
        xmlhttp.open("GET", 'status/'+vaultName+'/'+archiveId);
        xmlhttp.timeout = 30000;
        xmlhttp.send();
    }
};

function setStatus(index, status) {
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
