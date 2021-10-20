const ytpAutohide = document.querySelector('.ytp-autohide');
ytpAutohide.addEventListener('click', ev => {
    video.volume = 1;
    ytpAutohide.style.display = 'none';
})


let playing = false;
let timer = 0;
const player = document.querySelector('#player');
player.addEventListener('click', ev => {
    if (!playing) return;
    playerControlOverlay.style.display = 'block';
    playerControlOverlay.className = playerControlOverlay.className + ' fadein';

    timer = setTimeout(() => {
        playerControlOverlay.style.display = 'none';
    }, 5000)
})


const playerControlOverlay = document.querySelector('.player-control-overlay');

playerControlOverlay.addEventListener('click', ev => {

})


const playerControlPlayPauseIcon = document.querySelector('.player-control-play-pause-icon');
playerControlPlayPauseIcon.addEventListener('click', ev => {
    ev.stopPropagation();
    if (!video.paused) {
        video.pause();
        clearTimeout(timer);
        playing = false;
        playerControlPlayPauseIcon.querySelector('svg')
            .innerHTML = `<g>
                                        <path d="M6,4l12,8L6,20V4z"></path>
                                    </g>`;
    } else {
        video.play();
        timer = setTimeout(() => {
            playerControlOverlay.style.display = 'none';
        }, 5000)
        playerControlPlayPauseIcon.querySelector('svg')
            .innerHTML = `<path d="M9,19H7V5H9ZM17,5H15V19h2Z"></path>`;
    }
})


const iconButton = document.querySelector('.full-screen');
iconButton.addEventListener('click', async ev => {
    ev.stopPropagation();
    try {
        await video.requestFullscreen();//webkitRequestFullscreen();
        if (screen.orientation.lock)
            screen.orientation.lock("landscape");
    } catch (e) {
    }
})


video.addEventListener('abort', ev => {
    console.log('abort');
});

video.addEventListener('canplay', ev => {
    console.log('canplay');
});

video.addEventListener('canplaythrough', ev => {
    console.log('canplaythrough');
});

video.addEventListener('durationchange', ev => {
    console.log('durationchange');
    timeSecond.textContent = formatDuration(video.duration);
});

video.addEventListener('emptied', ev => {
    console.log('emptied');
});

video.addEventListener('ended', ev => {
    console.log('ended');
});

video.addEventListener('error', ev => {
    console.log('error');
});

video.addEventListener('loadeddata', ev => {
    console.log('loadeddata');
});

video.addEventListener('loadedmetadata', ev => {
    console.log('loadedmetadata');
});

video.addEventListener('loadstart', ev => {
    console.log('loadstart');
});

video.addEventListener('pause', ev => {
    console.log('pause');
});

video.addEventListener('play', ev => {
    ytpButton.style.display = 'none';
    console.log('play');
});

video.addEventListener('playing', ev => {
    playing = true;
    console.log('playing');
});

video.addEventListener('progress', ev => {
    console.log('progress');
    progressBarLoaded.style.width = calculateLoadedPercent(video);
});

video.addEventListener('ratechange', ev => {
    console.log('ratechange');
});

video.addEventListener('seeked ', ev => {
    console.log('seeked ');
});

video.addEventListener('seeking', ev => {
    console.log('seeking');
});

video.addEventListener('stalled', ev => {
    console.log('stalled');
});

video.addEventListener('suspend', ev => {
    console.log('suspend');
});

video.addEventListener('timeupdate', ev => {
    timeFirst.textContent = formatDuration(video.currentTime);
    const percent = calculateProgressPercent(video);
    progressBarPlayed.style.width = percent;
    progressBarPlayheadWrapper.style.marginLeft = percent;
});

video.addEventListener('volumechange', ev => {
    console.log('volumechange');
});

video.addEventListener('waiting', ev => {
    console.log('waiting');
});


const contextRenderer = document.querySelector('.context-renderer');

async function applyVideos() {
    async function getRandomVideos() {
        const response = await fetch("http://47.106.105.122/api/video/random");
        if (!response.ok) throw new Error(response.statusText);
        return await response.json();
    }


    new IntersectionObserver(entries => {
        if (entries[0].isIntersecting) {
            entries[0].target.src = entries[0].target.dataset.src;
        }
    });

    async function loadVideos() {
        const imageObserver = new IntersectionObserver(entries => {
            if (entries[0].isIntersecting) {
                entries[0].target.src = entries[0].target.dataset.src;
                imageObserver.observe(entries[0].target);
            }
        });

        async function getBaseUri() {
            const response = await fetch("http://47.106.105.122/api/video/57ck");
            if (!response.ok) throw new Error(response.statusText);
            return await response.text();
        }

        let baseUri = await getBaseUri();
        console.log(baseUri);

        const videos = await getRandomVideos();
        const documentFragment = document.createDocumentFragment();

        videos.forEach(v => {
            const ytmLargeMediaItem = document.createElement('DIV');
            ytmLargeMediaItem.setAttribute('data-id', v.id);
            ytmLargeMediaItem.setAttribute('data-href', v.url);
            ytmLargeMediaItem.setAttribute('class', 'ytm-large-media-item');
            const a = document.createElement('A');
            const videoThumbnailContainerLarge = document.createElement('DIV');
            videoThumbnailContainerLarge.setAttribute('class', 'video-thumbnail-container-large');
            const videoThumbnailBg = document.createElement('DIV');
            videoThumbnailBg.setAttribute('class', 'video-thumbnail-bg');
            videoThumbnailContainerLarge.appendChild(videoThumbnailBg);
            const ytmVideoWithContextRenderer = document.createElement('IMG');
            ytmVideoWithContextRenderer.setAttribute('class', 'ytm-video-with-context-renderer');
            ytmVideoWithContextRenderer.setAttribute('data-src', v.thumbnail);
            imageObserver.observe(ytmVideoWithContextRenderer);
            videoThumbnailContainerLarge.appendChild(ytmVideoWithContextRenderer);
            const videoThumbnailOverlayBottomGroup = document.createElement('DIV');
            videoThumbnailOverlayBottomGroup.setAttribute('class', 'video-thumbnail-overlay-bottom-group');
            const ytmThumbnailOverlayTimeStatusRenderer = document.createElement('DIV');
            ytmThumbnailOverlayTimeStatusRenderer.setAttribute('class', 'ytm-thumbnail-overlay-time-status-renderer');
            ytmThumbnailOverlayTimeStatusRenderer.appendChild(document.createTextNode(formatDuration(v.duration)));
            videoThumbnailOverlayBottomGroup.appendChild(ytmThumbnailOverlayTimeStatusRenderer);
            videoThumbnailContainerLarge.appendChild(videoThumbnailOverlayBottomGroup);
            a.appendChild(videoThumbnailContainerLarge);
            ytmLargeMediaItem.appendChild(a);
            const details = document.createElement('DIV');
            details.setAttribute('class', 'details');
            const largeMediaChannel = document.createElement('DIV');
            largeMediaChannel.setAttribute('class', 'large-media-channel');
            const ytmChannelThumbnailWithLinkRenderer = document.createElement('DIV');
            ytmChannelThumbnailWithLinkRenderer.setAttribute('class', 'ytm-channel-thumbnail-with-link-renderer');
            const channelThumbnailIcon = document.createElement('DIV');
            channelThumbnailIcon.setAttribute('class', 'channel-thumbnail-icon');
            channelThumbnailIcon.appendChild(document.createTextNode(v.type === 3 ? '57' : (v.type === 1 ? '91' : "XV")));
            ytmChannelThumbnailWithLinkRenderer.appendChild(channelThumbnailIcon);
            largeMediaChannel.appendChild(ytmChannelThumbnailWithLinkRenderer);
            details.appendChild(largeMediaChannel);
            const largeMediaItemInfo = document.createElement('DIV');
            largeMediaItemInfo.setAttribute('class', 'large-media-item-info');
            const largeMediaItemMetadata = document.createElement('DIV');
            largeMediaItemMetadata.setAttribute('class', 'large-media-item-metadata');
            const h3 = document.createElement('H3');
            h3.appendChild(document.createTextNode(v.title));
            largeMediaItemMetadata.appendChild(h3);
            largeMediaItemInfo.appendChild(largeMediaItemMetadata);
            const largeMediaItemMenu = document.createElement('DIV');
            largeMediaItemMenu.setAttribute('class', 'large-media-item-menu');
            const iconButton = document.createElement('BUTTON');
            iconButton.setAttribute('class', 'icon-button');
            const c3Icon = document.createElement('C3-ICON');
            const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
            svg.setAttribute('xmlns', 'http://www.w3.org/2000/svg');
            svg.setAttribute('enable-background', 'new 0 0 24 24');
            svg.setAttribute('height', '24');
            svg.setAttribute('viewBox', '0 0 24 24');
            svg.setAttribute('width', '24');
            const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
            path.setAttribute('d', 'M12,16.5c0.83,0,1.5,0.67,1.5,1.5s-0.67,1.5-1.5,1.5s-1.5-0.67-1.5-1.5S11.17,16.5,12,16.5z M10.5,12 c0,0.83,0.67,1.5,1.5,1.5s1.5-0.67,1.5-1.5s-0.67-1.5-1.5-1.5S10.5,11.17,10.5,12z M10.5,6c0,0.83,0.67,1.5,1.5,1.5 s1.5-0.67,1.5-1.5S12.83,4.5,12,4.5S10.5,5.17,10.5,6z');
            svg.appendChild(path);
            c3Icon.appendChild(svg);
            iconButton.appendChild(c3Icon);
            largeMediaItemMenu.appendChild(iconButton);
            largeMediaItemInfo.appendChild(largeMediaItemMenu);
            details.appendChild(largeMediaItemInfo);
            ytmLargeMediaItem.appendChild(details);

            ytmLargeMediaItem.addEventListener('click', ev => {
                const href = ytmLargeMediaItem.getAttribute('data-href');
                const id = ytmLargeMediaItem.getAttribute('data-id');
                fetch(`http://47.106.105.122/api/video/record?id=${id}`).then(res => res.text()).then(res => {
                    console.log(res);
                })
                if (href.startsWith("http://") || href.startsWith("https://"))
                    window.JInterface.parse(href);
                else
                    window.JInterface.parse(baseUri + href);
            });

            documentFragment.appendChild(ytmLargeMediaItem);
        });
        contextRenderer.appendChild(documentFragment);
    }

    await loadVideos();
}

applyVideos();
