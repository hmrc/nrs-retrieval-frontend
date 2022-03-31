
var timeout = 100;

const path = '/nrs-retrieval/';
const retrievalCompleteClass = "retrieval-complete"
const retrievalIncompleteClass = "retrieval-incomplete"
const retrievalFailedClass = "retrieval-failed"
const statusComplete = "Complete"
const statusIncomplete = "Incomplete"
const statusFailed = "Failed"
const get = "GET"

function checkStatus(index, vaultName, archiveId) {
  const xmlhttp = http(index, vaultName, archiveId);
  xmlhttp.open(get, path + 'status/' + vaultName + '/' + archiveId);
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
      setStatus(index, statusFailed)
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

function hide(element) {
  element.setAttribute("aria-hidden", true)
}

function show(element) {
  element.setAttribute("aria-hidden", false)
}

function busy(element, isBusy) {
  element.setAttribute("aria-busy", isBusy)
}

function setStatus(index, status) {
  const resultRetrieveElement = document.getElementById('result-retrieve-' + index)
  const retrievalIncompleteElement = document.getElementById('result-incomplete-' + index)
  const retrievalCompleteElement = document.getElementById('download-button-' + index)
  const startRetrievalElement = document.getElementById('start-retrieval-' + index)

  switch (status) {
    case statusComplete:
      busy(resultRetrieveElement, false)
      resultRetrieveElement.classList.add(retrievalCompleteClass);
      resultRetrieveElement.classList.remove(retrievalIncompleteClass);

      hide(retrievalIncompleteElement)
      show(retrievalCompleteElement)

      break;
    case statusFailed:
      busy(resultRetrieveElement, false)
      resultRetrieveElement.classList.add(retrievalFailedClass);
      resultRetrieveElement.classList.remove(retrievalIncompleteClass);

      hide(retrievalIncompleteElement)

      document.getElementsByClassName(retrievalFailedClass).forEach(show())

      break;
    default:
      busy(resultRetrieveElement, true)
      resultRetrieveElement.classList.add(retrievalIncompleteClass);

      hide(startRetrievalElement)
      show(retrievalIncompleteElement)
  }
}

function doRetrieve(index, vaultName, archiveId) {
  setStatus(index, statusIncomplete)
  const xmlhttp = http(index, vaultName, archiveId);

  xmlhttp.open(get, path + 'retrieve/' + vaultName + '/' + archiveId);
  xmlhttp.timeout = timeout;
  xmlhttp.send();
}

function startRetrieval(startRetrievalElement) {
  const dataIndex = startRetrievalElement.getAttribute("data-index")
  const dataVaultId = startRetrievalElement.getAttribute("data-vault-id")
  const dataArchiveId = startRetrievalElement.getAttribute("data-archive-id")

  doRetrieve(dataIndex, dataVaultId, dataArchiveId)
}

