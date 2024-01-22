package com.mrcrayfish.catalogue.client;

/**
 * Author: MrCrayfish
 */
public class ClientHelper
{
    public static boolean isMouseWithin(int x, int y, int width, int height, int mouseX, int mouseY)
    {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
