#include<stdio.h>
#include<stdlib.h>
int main(int argc, char* argv[])
{
	
	FILE* fp = fopen("test160.list", "wt+");
	for(int i = 0; i < 60; i++)
	{
		fprintf(fp, "./test1602/%d.txt\n", i);
	}
	fclose(fp);
	
	fp = fopen("dev160.list", "wt+");
        for(int i = 0; i < 30; i++)
        {
                fprintf(fp, "./dev1602/%d.txt\n", i);
        }
        fclose(fp);
	
	fp = fopen("train160.list", "wt+");
        for(int i = 0; i < 70; i++)
        {
                fprintf(fp, "./train1602/%d.txt\n", i);
        }
        fclose(fp);
	
	fp = fopen("test500.list", "wt+");
        for(int i = 0; i < 150; i++)
        {
                fprintf(fp, "./test5002/%d.txt\n", i);
        }
        fclose(fp);

	fp = fopen("dev500.list", "wt+");
        for(int i = 0; i < 50; i++)
        {
                fprintf(fp, "./dev5002/%d.txt\n", i);
        }
        fclose(fp);

	fp = fopen("train500.list", "wt+");
        for(int i = 0; i < 300; i++)
        {
                fprintf(fp, "./train5002/%d.txt\n", i);
        }
        fclose(fp);	
}
