# FFmpeg

```
ffmpeg -ss 0 -t 20 -i input.mp4 -vf "fps=2,scale=320:-1:flags=lanczos,split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse" -loop 0 output.gif

ffmpeg -i YourFile.mp4 -ss 00:10:25 -t 00:00:05 -acodec copy -vcodec copy Output.mp4


```

- https://github.com/FFmpeg/FFmpeg
