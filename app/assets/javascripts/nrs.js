
var timeout = 1000;

const PATH = '/nrs-retrieval/';
const RETRIEVAL_COMPLETE_CLASS = "retrieval-complete"
const RETRIEVAL_DOWNLOADING_CLASS = "retrieval-downloading"
const RETRIEVAL_INCOMPLETE_CLASS = "retrieval-incomplete"
const RETRIEVAL_FAILED_CLASS = "retrieval-failed"
const STATUS_DOWNLOADED = "Downloaded"
const STATUS_COMPLETE = "Complete"
const STATUS_INCOMPLETE = "Incomplete"
const STATUS_FAILED = "Failed"
const GET = "GET"
const FAILEDTIMEOUT = document.getElementById("timeout").value

const GOVUK_VISUALLY_HIDDEN = "govuk-visually-hidden"
function checkStatus(index, vaultName, archiveId, requestTimeout) {
  if (requestTimeout !== 0) {
    const xmlhttp = http(index, vaultName, archiveId, requestTimeout);
    xmlhttp.open(GET, PATH + 'status/' + vaultName + '/' + archiveId);
    xmlhttp.timeout = requestTimeout;
    xmlhttp.send();
  }
}

function http(index, vaultName, archiveId, requestTimeout) {
  const xmlhttp = new XMLHttpRequest();

  xmlhttp.onload = function (e) {
    if (xmlhttp.readyState === 4 && xmlhttp.status === 200) {
      setStatus(index, this.response)
    } else if (xmlhttp.status === 500) {
      setStatus(index, STATUS_FAILED)
    } else {
      const updatedRequestTimeout = shouldRetry(index, requestTimeout);

      setTimeout(function () {
        checkStatus(index, vaultName, archiveId, updatedRequestTimeout)
      }, updatedRequestTimeout)
    }
  };

  xmlhttp.ontimeout = function () {
    const updatedRequestTimeout = shouldRetry(index, requestTimeout);

    checkStatus(index, vaultName, archiveId, updatedRequestTimeout);
  };

  return xmlhttp;
}

function hide(element) {
  element.setAttribute("aria-hidden", true)
  element.setAttribute("style", "display:none")
  element.classList.add(GOVUK_VISUALLY_HIDDEN)
}

function show(element) {
  element.setAttribute("aria-hidden", false)
  element.removeAttribute("style")
  element.classList.remove(GOVUK_VISUALLY_HIDDEN)
}

function busy(element, isBusy) {
  element.setAttribute("aria-busy", isBusy)
}

function setStatus(index, status) {
  const resultRetrieveElement = document.getElementById('result-retrieve-' + index)
  const resultErrorElement = document.getElementById('retrieval-failed-' + index)
  const retrievalIncompleteElement = document.getElementById('result-incomplete-' + index)
  const retrievalCompleteElement = document.getElementById('download-button-' + index)
  const retrievalCompleteButtonElement = document.getElementById('download-button-' + index + '-link')
  const retrievalDownloadedElement = document.getElementById('download-button-clicked-' + index)
  const startRetrievalElement = document.getElementById('start-retrieval-' + index)
  const startRetrievalButtonElement = document.getElementById('start-retrieval-' + index + '-link')

  switch (status) {
    case STATUS_COMPLETE:
      busy(resultRetrieveElement, false)
      resultRetrieveElement.classList.add(RETRIEVAL_COMPLETE_CLASS);
      resultRetrieveElement.classList.remove(RETRIEVAL_INCOMPLETE_CLASS);

      hide(retrievalIncompleteElement)
      show(retrievalCompleteElement)
      show(retrievalCompleteButtonElement)

      break;
    case STATUS_FAILED:
      busy(resultRetrieveElement, false)
      resultRetrieveElement.classList.add(RETRIEVAL_FAILED_CLASS);
      resultRetrieveElement.classList.remove(RETRIEVAL_INCOMPLETE_CLASS);

      hide(retrievalIncompleteElement)

      show(resultErrorElement)

      break;
      case STATUS_DOWNLOADED:
      busy(resultRetrieveElement, false)
        resultRetrieveElement.classList.add(RETRIEVAL_DOWNLOADING_CLASS);
        resultRetrieveElement.classList.remove(RETRIEVAL_COMPLETE_CLASS);

      hide(retrievalCompleteElement)
      show(retrievalDownloadedElement)
      break;
    default:
      busy(resultRetrieveElement, true)
      resultRetrieveElement.classList.add(RETRIEVAL_INCOMPLETE_CLASS);

      hide(startRetrievalElement)
      hide(startRetrievalButtonElement)
      show(retrievalIncompleteElement)
  }
}

function shouldRetry(index, requestTimeout) {
  const predicate = isPending(index)

  if (predicate) {
    return Math.min(requestTimeout * 2, 5000)
  } else {
    return 0
  }
}

function doRetrieve(index, vaultName, archiveId) {
  setStatus(index, STATUS_INCOMPLETE)
  const xmlhttp = http(index, vaultName, archiveId, timeout);

  xmlhttp.open(GET, PATH + 'retrieve/' + notableEvent.name + '/' + vaultName + '/' + archiveId);
  xmlhttp.timeout = timeout;
  xmlhttp.send();

  setTimeout(function () {
    const predicate = isPending(index)

    if (predicate) {
      setStatus(index, STATUS_FAILED)
    }
  }, FAILEDTIMEOUT)
}

function isPending(index) {
  const resultRetrieveElement = document.getElementById('result-retrieve-' + index)

  return resultRetrieveElement.getAttribute("aria-busy") === "true"
}

function startRetrieval(startRetrievalElement) {
  const dataIndex = startRetrievalElement.getAttribute("data-index")
  const dataVaultId = startRetrievalElement.getAttribute("data-vault-id")
  const dataArchiveId = startRetrievalElement.getAttribute("data-archive-id")

  doRetrieve(dataIndex, dataVaultId, dataArchiveId)
}