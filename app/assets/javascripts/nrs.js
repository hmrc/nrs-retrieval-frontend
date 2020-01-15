/* global jQuery */
/* global GOVUK */
(function ($) {

  // Use GOV.UK shim-links-with-button-role.js to trigger a link styled to look like a button,
  // with role="button" when the space key is pressed.
  GOVUK.shimLinksWithButtonRole.init()

  var $errorSummary = $('.error-summary');

  if ($errorSummary.length) {
    // summary focusing
    $errorSummary.focus();
    $('.error-summary-list li a').each(function (i, item) {
      var $link = $(item);
      // error focusing
      $link.on('click', function () {
        var target = $(this).attr('href').slice(1);
        window.setTimeout(function () {
          $('#' + target)
            .parent()
            .find('input.form-control-error')
            .first()
            .focus()
        }, 200)
      })

    })
  }

  var timeout = 100
  var path = '/nrs-retrieval/';

  var NrsAjax = {
    http: function (index, vaultName, archiveId) {
      var xmlhttp = new XMLHttpRequest();
      xmlhttp.onload = function (e) {
        if (xmlhttp.readyState === 4 && xmlhttp.status === 200) {
          setStatus(index, this.response)
        } else if (xmlhttp.status === 500) {
          showServerError(index)
        } else {
          setTimeout(function () {
            NrsAjax.checkStatus(index, vaultName, archiveId)
          }, timeout)
        }
      };
      xmlhttp.ontimeout = function () {
        NrsAjax.checkStatus(index, vaultName, archiveId);
      };
      return xmlhttp;
    },
    checkStatus: function (index, vaultName, archiveId) {
      var xmlhttp = this.http(index, vaultName, archiveId);
      xmlhttp.open("GET", path + 'status/' + vaultName + '/' + archiveId);
      xmlhttp.timeout = timeout;
      xmlhttp.send();
      timeout = Math.min(timeout * 2, 5000)
    },
    doRetrieve: function (index, vaultName, archiveId) {
      timeout = 100
      setStatus(index, 'retrieval-incomplete')
      var xmlhttp = this.http(index, vaultName, archiveId);
      xmlhttp.open("GET", path + 'retrieve/' + vaultName + '/' + archiveId);
      xmlhttp.timeout = timeout;
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

  var $resultsInfo = $('h2[role="alert"]');

  if ($resultsInfo.length) {
    $resultsInfo.focus()
  }

})(jQuery);
