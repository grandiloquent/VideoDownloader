if (window.location.hostname === "91porn.com") {
    const body = document.createElement('body');
    const row = document.querySelector('#wrapper .row');
    const paging = document.getElementById('paging');
    document.body.replaceWith(body);
    if (row) {
        row.style.margin = '0';
        body.appendChild(row);
        if (paging) {
            body.appendChild(paging)
        }
        const style = document.createElement('style');
        style.textContent = "body{padding:0!important}";
        document.querySelector('head').appendChild(style)
    }
}
if (window.location.hostname === "m.iqiyi.com") {
    const mIqyGuideLayer = document.querySelector(".m-iqyGuide-layer");
    if (mIqyGuideLayer) {
        mIqyGuideLayer.style.display = 'none'
    }
}
if (window.location.hostname.indexOf("xvideos.com") !== -1) {
    const exOverTop = document.querySelector(".ex-over-top");
    if (exOverTop) {
        exOverTop.remove()
    }
    const adHeaderMobileContener = document.querySelector("#ad-header-mobile-contener");
    if (adHeaderMobileContener) {
        adHeaderMobileContener.remove()
    }
    const adFooter = document.querySelector("#ad-footer");
    if (adFooter) {
        adFooter.remove()
    }
    const thumbAd = [
        ...document.querySelectorAll(".thumb-ad,.video-ad")
    ];
    thumbAd.forEach(i => {
        i.remove()
    })
}
if (window.location.hostname.indexOf("pornone.com") !== -1) {
    const ciwShow = document.querySelector('._ciw-show__');
    if (ciwShow) {
        ciwShow.remove()
    }
}
if (window.location.hostname.indexOf("qq.com") !== -1) {
    const atAppBanner = document.querySelector('.at-app-banner');
    if (atAppBanner) {
        atAppBanner.style.display = "none"
    }
}
if (window.location.hostname.indexOf("bilibili.com") !== -1) {
    const launchAppBtns = document.querySelectorAll('.launch-app-btn');
    launchAppBtns.forEach(launchAppBtn => launchAppBtn.remove())
}
if (window.location.hostname.indexOf("acfun.cn") !== -1) {
    const headerIco = document.querySelector('.header-ico');
    if (headerIco) {
        headerIco.remove()
    }
    const commonInvokePanel = document.querySelector('#common_invoke_panel');
    if (commonInvokePanel) {
        commonInvokePanel.remove()
    }
    const downAppButton = document.querySelector('.down-app-button');
    if (downAppButton) {
        downAppButton.remove()
    }
    const openAppBtns = document.querySelectorAll('.open-app-btn');
    openAppBtns.forEach(openAppBtn => {
        openAppBtn.remove()
    });
    const commonProfitFixeds = document.querySelectorAll('.common_profit_fixed');
    commonProfitFixeds.forEach(commonProfitFixed => {
        commonProfitFixed.remove()
    })
}