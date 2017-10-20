function copyright(selector, year) {
    var currentYear = (new Date()).getFullYear();

    var yearText = year === currentYear ? currentYear : (year + " - " + currentYear);
    $(selector).html("<span id='copyright'>&copy; " + yearText+ " <a class='copyright-link' href='https://dragonbra.in' target='_blank'>dragonbra.in</a></span>");
}