function formatDuration(ms) {
    if (isNaN(ms)) return '0:00';
    if (ms < 0) ms = -ms;
    const time = {
        hour: Math.floor(ms / 3600) % 24,
        minute: Math.floor(ms / 60) % 60,
        second: Math.floor(ms) % 60,
    };
    return Object.entries(time)
        .filter((val, index) => index || val[1])
        .map(val => (val[1] + '').padStart(2, '0'))
        .join(':');
}

function calculateLoadedPercent(video) {
    if (!video.buffered.length) {
        return '0';
    }
    return (video.buffered.end(0) / video.duration) * 100 + '%';
}

function calculateProgressPercent(video) {
    return ((video.currentTime / video.duration) * 100).toFixed(2) + '%';
}

function start(obj) {
    const videos = JSON.parse(obj).videos;
    video.src = videos[0];
    video.play();
}

const timeFirst = document.querySelector('.time-first');
const timeSecond = document.querySelector('.time-second');
const progressBarPlayed = document.querySelector('.progress-bar-played');
const progressBarLoaded = document.querySelector('.progress-bar-loaded');
const progressBarPlayheadWrapper = document.querySelector('.progress-bar-playhead-wrapper');

const video = document.querySelector('.html5-main-video');
video.volume = 0;

const ytpButton = document.querySelector('.ytp-button');
ytpButton.addEventListener('click', ev => {
    ev.stopPropagation();
    video.play();
    ytpButton.style.display = 'none';
})

