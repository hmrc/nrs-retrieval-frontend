(function ($) {

  var NrsAjax = {
    http: function (index, vaultName, archiveId) {
      var xmlhttp = new XMLHttpRequest();
      xmlhttp.onload = function (e) {
        if (xmlhttp.readyState === 4 && xmlhttp.status === 200) {
          setStatus(index, this.response)
        } else if (xmlhttp.status === 500) {
          showServerError(index)
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
      xmlhttp.timeout = 5000;
      xmlhttp.send();
    },
    doRetrieve: function (index, vaultName, archiveId) {
      setStatus(index, 'retrieval-incomplete')
      var xmlhttp = this.http(index, vaultName, archiveId);
      xmlhttp.open("GET", 'retrieve/' + vaultName + '/' + archiveId);
      xmlhttp.timeout = 30000;
      xmlhttp.send();
    }
  };

  function showServerError(index) {
    var $target = $('#retrieve' + index)
    $target.find('.error-message').remove()
    $target.prepend('<span class="error-message">Server is returning error HTTP 500</span>')
  }

  function setStatus(index, status) {
    var $target = $('#retrieve' + index).find('.result-retrieve')
    switch (status) {
      case 'Complete':
        $target
          .attr('aria-busy', false)
          .toggleClass('retrieval-incomplete retrieval-complete')
          .find('.retrieval-complete')
            .attr('aria-hidden', false)
            .end()
          .find('.retrieval-incomplete, .start-retrieval')
            .attr('aria-hidden', true)
        break;
      case 'Failed':
        $target
          .attr('aria-busy', false)
          .toggleClass('retrieval-incomplete retrieval-failed')
          .find('.retrieval-failed')
            .attr('aria-hidden', false)
            .end()
          .find('.retrieval-incomplete, .start-retrieval')
            .attr('aria-hidden', true)
        break;
      default:
        $target
          .attr('aria-busy', true)
          .addClass('retrieval-incomplete')
          .find('.retrieval-incomplete')
            .attr('aria-hidden', false)
            .end()
          .find('.start-retrieval')
            .attr('aria-hidden', true)
    }
  }

  // attach listeners to retrieval events
  $('a.start-retrieval').on('click', function (e) {
    e.preventDefault()
    var data = $(e.currentTarget).data()
    NrsAjax.doRetrieve(data.index, data.vaultId, data.archiveId)
  })

})(jQuery);
