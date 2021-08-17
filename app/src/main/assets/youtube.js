//[...document.querySelectorAll('ytm-promoted-sparkles-web-renderer,.GoogleActiveViewElement')].forEach(x=>x.remove())

if (window.location.hostname === "91porn.com") {
    const body = document.createElement('body');
    const element = document.querySelector('#wrapper .row');
    if (element) {
        body.appendChild(element);
        document.body.replaceWith(body);
    }
    element.style.margin = '0';
    body.style.padding = '0';
}
