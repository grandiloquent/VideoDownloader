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
if (window.location.hostname.indexOf("xvideos.com") !== -1) {
    const exOverTop = document.querySelector(".ex-over-top");
    if (exOverTop) {
        exOverTop.remove();
    }

    const adHeaderMobileContener = document.querySelector("#ad-header-mobile-contener");
    if (adHeaderMobileContener) {
        adHeaderMobileContener.remove();
    }

    const adFooter = document.querySelector("#ad-footer");
    if (adFooter) {
        adFooter.remove();
    }
    const thumbAd = [...document.querySelectorAll(".thumb-ad,.video-ad")];
    thumbAd.forEach(i => {
        i.remove();
    });

}
if (window.location.hostname.indexOf("pornone.com") !== -1) {
    const ciwShow = document.querySelector('._ciw-show__');
    if (ciwShow) {
        ciwShow.remove();
    }
}

if (window.location.hostname.indexOf("qq.com") !== -1) {
    const atAppBanner = document.querySelector('.at-app-banner');
    if (atAppBanner) {
        atAppBanner.style.display="none";
    }
}
