#ifndef TIMER_H
#define TIMER_H
// #include "timer.h"
#define TIME_FORMAT_STR_LEN (20)

struct Timer {
    struct timeval end_time;
};

typedef struct Timer Timer;

bool expired(Timer *timer) {
    struct timeval now, res;
    gettimeofday(&now, NULL);
    timersub(&timer->end_time, &now, &res);
    return res.tv_sec < 0 || (res.tv_sec == 0 && res.tv_usec <= 0);
}

void countdown_ms(Timer *timer, unsigned int timeout_ms) {
    struct timeval now;
    gettimeofday(&now, NULL);
    struct timeval interval = {timeout_ms / 1000, (timeout_ms % 1000) * 1000};
    timeradd(&now, &interval, &timer->end_time);
}

void HAL_Timer_countdown(Timer *timer, unsigned int timeout) {
    struct timeval now;
    gettimeofday(&now, NULL);
    struct timeval interval = {timeout, 0};
    timeradd(&now, &interval, &timer->end_time);
}

int HAL_Timer_remain(Timer *timer) {
    struct timeval now, res;
    gettimeofday(&now, NULL);
    timersub(&timer->end_time, &now, &res);
    return (res.tv_sec < 0) ? 0 : res.tv_sec * 1000 + res.tv_usec / 1000;
}

void timer_init(Timer *timer) {
    timer->end_time = (struct timeval) {0, 0};
}

char *HAL_Timer_current(char *time_str) {
    if (time_str == NULL)
        return " ";

    struct timeval tv;
    gettimeofday(&tv, NULL);
    time_t now_time = tv.tv_sec;

    struct tm tm_tmp = *localtime(&now_time);
    strftime(time_str, TIME_FORMAT_STR_LEN, "%F %T", &tm_tmp);

    return time_str;
}

long HAL_Timer_current_sec(void) {
    struct timeval tv;
    gettimeofday(&tv, NULL);

    return tv.tv_sec;
}

#endif
