
var timeout = 1000;

const PATH = '/nrs-retrieval/';
const RETRIEVAL_COMPLETE_CLASS = "retrieval-complete"
const RETRIEVAL_DOWNLOADING_CLASS = "retrieval-downloading"
const RETRIEVAL_INCOMPLETE_CLASS = "retrieval-incomplete"
const RETRIEVAL_FAILED_CLASS = "retrieval-failed"
const STATUS_COMPLETE = "Complete"
const STATUS_INCOMPLETE = "Incomplete"
const STATUS_FAILED = "Failed"
const GET = "GET"

function checkStatus(index, vaultName, archiveId) {
  const xmlhttp = http(index, vaultName, archiveId);
  xmlhttp.open(GET, PATH + 'status/' + vaultName + '/' + archiveId);
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
      setStatus(index, STATUS_FAILED)
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
    case STATUS_COMPLETE:
      busy(resultRetrieveElement, false)
      resultRetrieveElement.classList.add(RETRIEVAL_COMPLETE_CLASS);
      resultRetrieveElement.classList.remove(RETRIEVAL_INCOMPLETE_CLASS);

      hide(retrievalIncompleteElement)
      show(retrievalCompleteElement)

      break;
    case STATUS_FAILED:
      busy(resultRetrieveElement, false)
      resultRetrieveElement.classList.add(RETRIEVAL_FAILED_CLASS);
      resultRetrieveElement.classList.remove(RETRIEVAL_INCOMPLETE_CLASS);

      hide(retrievalIncompleteElement)

      document.getElementsByClassName(RETRIEVAL_FAILED_CLASS).forEach(show())

      break;
    default:
      busy(resultRetrieveElement, true)
      resultRetrieveElement.classList.add(RETRIEVAL_INCOMPLETE_CLASS);

      hide(startRetrievalElement)
      show(retrievalIncompleteElement)
  }
}

function doRetrieve(index, vaultName, archiveId) {
  setStatus(index, STATUS_INCOMPLETE)
  const xmlhttp = http(index, vaultName, archiveId);

  xmlhttp.open(GET, PATH + 'retrieve/' + vaultName + '/' + archiveId);
  xmlhttp.timeout = timeout;
  xmlhttp.send();
}

function startRetrieval(startRetrievalElement) {
  const dataIndex = startRetrievalElement.getAttribute("data-index")
  const dataVaultId = startRetrievalElement.getAttribute("data-vault-id")
  const dataArchiveId = startRetrievalElement.getAttribute("data-archive-id")

  doRetrieve(dataIndex, dataVaultId, dataArchiveId)
}

function doDownload(downloadRetrievalElement) {
  console.log("triggered")
  const dataIndex = downloadRetrievalElement.getAttribute("data-index")
  const resultRetrieveElement = document.getElementById('result-retrieve-' + dataIndex)
  const downloadingElement = document.getElementById('download-button-clicked-' + dataIndex)

  resultRetrieveElement.classList.add(RETRIEVAL_DOWNLOADING_CLASS);
  resultRetrieveElement.classList.remove(RETRIEVAL_COMPLETE_CLASS);

  hide(downloadRetrievalElement)
  show(downloadingElement)
}