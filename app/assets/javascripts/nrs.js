
var timeout = 100;
var path = '/nrs-retrieval/';

function checkStatus(index, vaultName, archiveId) {
  const xmlhttp = http(index, vaultName, archiveId);
  xmlhttp.open("GET", path + 'status/' + vaultName + '/' + archiveId);
  xmlhttp.timeout = timeout;
  xmlhttp.send();
  timeout = Math.min(timeout * 2, 5000)
}

function http(index, vaultName, archiveId) {
  const xmlhttp = new XMLHttpRequest();

  xmlhttp.onload = function (e) {
    if (xmlhttp.readyState === 4 && xmlhttp.status === 200) {
      setStatus(index, this.response)
    } else if (xmlhttp.status === 500) {
      setStatus(index, 'Failed')
    } else {
      setTimeout(function () {
        checkStatus(index, vaultName, archiveId)
      }, timeout)
    }
  };

  xmlhttp.ontimeout = function () {
    checkStatus(index, vaultName, archiveId);
  };

  return xmlhttp;
}

function show(item) {
  item.setAttribute("aria-hidden", false)
}

function setStatus(index, status) {
  var resultRetrieveElement = document.getElementById('result-retrieve-' + index)
  var retrievalIncompleteElement = document.getElementById('result-incomplete-' + index)
  var retrievalCompleteElement = document.getElementById('download-button-' + index)
  var startRetrievalElement = document.getElementById('start-retrieval-' + index)

  switch (status) {
    case 'Complete':
      resultRetrieveElement.setAttribute("aria-busy", false)
      resultRetrieveElement.classList.add("retrieval-complete");
      resultRetrieveElement.classList.remove("retrieval-incomplete");

      retrievalCompleteElement.setAttribute("aria-hidden", false)

      retrievalIncompleteElement.setAttribute("aria-hidden", true)

      break;
    case 'Failed':
      resultRetrieveElement.setAttribute("aria-busy", false)
      resultRetrieveElement.classList.add("retrieval-failed");
      resultRetrieveElement.classList.remove("retrieval-incomplete");

      retrievalIncompleteElement.setAttribute("aria-hidden", true)

      document.getElementsByClassName("retrieval-failed").forEach(show())

      break;
    default:
      resultRetrieveElement.setAttribute("aria-busy", true)
      resultRetrieveElement.classList.add("retrieval-incomplete");

      retrievalIncompleteElement.setAttribute("aria-hidden", false)

      startRetrievalElement.setAttribute("aria-hidden", true)
  }
}

function doRetrieve(index, vaultName, archiveId) {
  setStatus(index, 'retrieval-incomplete')
  var xmlhttp = http(index, vaultName, archiveId);

  xmlhttp.open("GET", path + 'retrieve/' + vaultName + '/' + archiveId);
  xmlhttp.timeout = timeout;
  xmlhttp.send();
}

function startRetrieval(startRetrievalElement) {
  var dataIndex = startRetrievalElement.getAttribute("data-index")
  var dataVaultId = startRetrievalElement.getAttribute("data-vault-id")
  var dataArchiveId = startRetrievalElement.getAttribute("data-archive-id")

  doRetrieve(dataIndex, dataVaultId, dataArchiveId)
}

