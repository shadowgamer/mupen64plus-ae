/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2012 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with Mupen64PlusAE. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae.input.provider;

import android.annotation.TargetApi;
import android.view.InputDevice;
import android.view.InputDevice.MotionRange;
import android.view.MotionEvent;
import android.view.View;

/**
 * A class for transforming Android MotionEvent inputs into a common format.
 */
public class AxisProvider extends AbstractProvider
{
    /** The input codes to listen for. */
    private int[] mInputCodes;
    
    /** The default number of input codes to listen for. */
    private static final int DEFAULT_NUM_INPUTS = 128;
    
    /**
     * Instantiates a new axis provider.
     * 
     * @param view The view receiving MotionEvent data.
     */
    @TargetApi( 12 )
    public AxisProvider( View view )
    {
        // By default, provide data from all possible axes
        mInputCodes = new int[DEFAULT_NUM_INPUTS];
        for( int i = 0; i < mInputCodes.length; i++ )
            mInputCodes[i] = -( i + 1 );
        
        // Connect the input source
        view.setOnGenericMotionListener( new GenericMotionListener() );
        
        // Request focus for proper listening
        view.requestFocus();
    }
    
    /**
     * Restricts listening to a set of universal input codes.
     * 
     * @param inputCodeFilter The new input codes to listen for.
     */
    public void setInputCodeFilter( int[] inputCodeFilter )
    {
        mInputCodes = inputCodeFilter.clone();
    }
    
    /**
     * Just an indirection class that eliminates some logcat chatter about benign errors. If we make
     * the parent class implement View.OnGenericMotionListener, then we get logcat error messages
     * <i>even if</i> we conditionally exclude calls to the class based on API. These errors are not
     * actually harmful, so the logcat messages are simply a nuisance during debugging.
     * <p/>
     * For a detailed explanation, see <a href=http://stackoverflow.com/questions/13103902/
     * android-recommended-way-of-safely-supporting-newer-apis-has-error-if-the-class-i>here</a>.
     */
    public class GenericMotionListener implements View.OnGenericMotionListener
    {
        /*
         * (non-Javadoc)
         * 
         * @see android.view.View.OnGenericMotionListener#onGenericMotion(android.view.View,
         * android.view.MotionEvent)
         */
        @TargetApi( 12 )
        @Override
        public boolean onGenericMotion( View v, MotionEvent event )
        {
            InputDevice device = event.getDevice();
            
            // Read all the requested axes
            float[] strengths = new float[mInputCodes.length];
            for( int i = 0; i < mInputCodes.length; i++ )
            {
                int inputCode = mInputCodes[i];
                
                // Compute the axis code from the input code
                int axisCode = inputToAxisCode( inputCode );
                
                // Get the analog value using the Android API
                float strength = event.getAxisValue( axisCode );
                MotionRange motionRange = device.getMotionRange( axisCode );
                if( motionRange != null )
                    strength = 2f * ( strength - motionRange.getMin() ) / motionRange.getRange() - 1f;
                
                // If the strength points in the correct direction, record it
                boolean direction1 = inputToAxisDirection( inputCode );
                boolean direction2 = strength > 0;
                if( direction1 == direction2 )
                    strengths[i] = Math.abs( strength );
                else
                    strengths[i] = 0;
            }
            
            // Notify listeners about new input data
            notifyListeners( mInputCodes, strengths, getHardwareId( event ) );
            
            return true;
        }
    }
}