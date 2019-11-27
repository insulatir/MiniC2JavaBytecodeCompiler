int func(int t) {
   
   int sum = 0;
   if((t==100)){
     while(sum <= 100){
         ++sum;
      }
   }
   return sum;
}

void main () {
   int t = 100;  
   _print(func(t));
   
   return;
}