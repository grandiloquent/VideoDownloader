//[...document.querySelectorAll('ytm-promoted-sparkles-web-renderer,.GoogleActiveViewElement')].forEach(x=>x.remove())

if (window.location.hostname === "91porn.com") {
    const body = document.createElement('body');
    const row = document.querySelector('#wrapper .row');
    const paging = document.getElementById('paging');
    document.body.replaceWith(body);

    if (row) {
        row.style.margin = '0';
        body.appendChild(row);

        if (paging) {
            body.appendChild(paging);
        }


        const style = document.createElement('style');
        style.textContent = "body{padding:0!important}";
        document.querySelector('head').appendChild(style);
    }
}
if (window.location.hostname === "m.iqiyi.com") {
    const mIqyGuideLayer = document.querySelector(".m-iqyGuide-layer");
    if (mIqyGuideLayer) {
        mIqyGuideLayer.style.display = 'none';
    }
}

