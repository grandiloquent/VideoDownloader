#include <time.h> 

static inline int randomIP(int begin, int end) {
    int gap = end - begin + 1;
    int ret = 0;
    //srand((unsigned) time(0));
    ret = rand() % gap + begin;
//in++;
    return ret;
}
int main(int argc, char const *argv[])
{
  srand((unsigned)time(0));
for (int i = 0; i < 10; ++i)
{
  printf("%d\n", randomIP(1, 255));
}
  return 0;
}