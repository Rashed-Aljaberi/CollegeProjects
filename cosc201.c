#include <stdio.h>

void sim(int tags[],int lines){
	FILE *ptr; //pointer to file
	ptr = fopen("trace.txt","r"); //open trace file
	int miss=0,hit=0; //holds total misses and hits
	char address[8]; //holds the addrress aquired from the file
	int i=0; //counter for line in file
	while(i<lines){
		fgets(address,9, ptr); //gets a line from text file and places it in address
		unsigned int n = strtol(address, NULL, 16), //convert address taken from "trace.txt" and placed in 'address' from char to int
		offset=n & 0x3f, //f=get block offset of address
		set=(n>>6)&0x1ff, //get index/set/line/block
		tag=n>>15; //get tag
		if(tags[set]==tag){ //checks if the tags bits match the tag at the index for a hit
			printf("cahce hit for %x at line numebr %d block offset %d\n",n,set,offset);
			hit++; //update hit counter
		}
		else{ //otherwise we got a miss
			printf("cache miss at %x, updating cache by moving respecitve page to line %d\n",n,set);
			miss++; //update miss counter
			tags[set]=tag; //update tag at the index so that the new page is moved
		}
		fgets(address,9, ptr); //after every read, fgets gives a gibbirish value, so this inscures it ignores it and only read every other
		time.
		i++; //update counter
	}
	fclose(ptr); //close trace file
	printf("there was a total of %d hits and %d misses and %d accesses. the program also updated the cache %d times, the same number as
	the misses\n",hit,miss,hit+miss,miss);
}

int main(){
	int addresses=100; //numbers of adresses to be computed from trace file
	int tags[512]={}; //holds the tags of all 512 blocks
	sim(tags,addresses); //runs the simulation
}