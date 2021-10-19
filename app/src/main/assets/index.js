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
    }else {
        video.play();
        timer = setTimeout(() => {
            playerControlOverlay.style.display = 'none';
        }, 5000)
        playerControlPlayPauseIcon.querySelector('svg')
            .innerHTML = `<path d="M9,19H7V5H9ZM17,5H15V19h2Z"></path>`;
    }
})



const iconButton = document.querySelector('.icon-button');
iconButton.addEventListener('click', async ev => {
    ev.stopPropagation();
    try {
        await video.requestFullscreen();//webkitRequestFullscreen();
        // if (screen.orientation.lock)
        //     screen.orientation.lock("landscape");
    } catch (e) {
        console.log(e);
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
