//[...document.querySelectorAll('ytm-promoted-sparkles-web-renderer,.GoogleActiveViewElement')].forEach(x=>x.remove())

if (window.location.hostname === "91porn.com") {
    const body = document.createElement('body');
    const row = document.querySelector('#wrapper .row');
    if (row) {
        row.style.margin = '0';
        body.appendChild(row);
    }
    const paging = document.getElementById('paging');
    if (paging) {
        body.appendChild(row);
    }
    document.body.replaceWith(body);


    const style = document.createElement('style');
    style.textContent = "body{padding:0!important}";
    document.querySelector('head').appendChild(style);
}
