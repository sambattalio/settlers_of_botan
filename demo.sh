trap ctrl_c INT

function ctrl_c() {
  printf "Cleaning up\n"
  pgrep -f "lib/jsettlers2/build/libs/JSettlers-2.2.00.jar" | xargs kill
}

make --no-print-directory build &&
make --no-print-directory simulate &
make --no-print-directory run