(function ($, window) {

  var NrsAjax = {
    http: function (index, vaultName, archiveId) {
      console.log('NrsAjax.http(' + index + ', ' + vaultName + ', ' + archiveId + ')')
      var xmlhttp = new XMLHttpRequest();
      xmlhttp.onload = function (e) {
        if (xmlhttp.readyState === 4 && xmlhttp.status === 200) {
          setStatus(index, this.response)
        } else {
          NrsAjax.checkStatus(index, vaultName, archiveId);
        }
      };
      xmlhttp.ontimeout = function () {
        NrsAjax.checkStatus(index, vaultName, archiveId);
      };
      return xmlhttp;
    },
    checkStatus: function (index, vaultName, archiveId) {
      var xmlhttp = this.http(index, vaultName, archiveId);
      xmlhttp.open("GET", 'status/' + vaultName + '/' + archiveId);
      xmlhttp.timeout = 30000;
      xmlhttp.send();
    }
  };

  function setStatus(index, status) {
    var $target = $('#retrieve' + index).find('.result-retrieve')
    switch (status) {
      case 'Complete':
        $target.addClass('retrieval-complete')
        break;
      case 'Failed':
        $target.addClass('retrieval-failed')
        break;
      default:
        $target.addClass('retrieval-in-progress')
    }
  }

  // attach listeners to retrieval events
  $('a.start-retrieval').on('click', function (e) {
    e.preventDefault();
    var $link = $(e.currentTarget)
    var vaultName = $link.attr("data-vault-id")
    var index = $link.attr("data-index")
    var archiveId = $link.attr("data-archive-id")
    setStatus(index, 'retrieval-in-progress')
    NrsAjax.checkStatus(index,  vaultName, archiveId)
  })

})(jQuery, window);
