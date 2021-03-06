package paulscode.android.mupen64plusae;

import java.io.File;
import java.util.List;

import paulscode.android.mupen64plusae.input.map.HardwareMap;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.Prompt;
import paulscode.android.mupen64plusae.util.SafeMethods;
import paulscode.android.mupen64plusae.util.Prompt.OnFileListener;
import paulscode.android.mupen64plusae.util.Prompt.OnTextListener;
import paulscode.android.mupen64plusae.util.Utility;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;

public class GameMenuHandler
{
    private static final int BASELINE_SPEED_FACTOR = 100;
    
    private static final int DEFAULT_SPEED_FACTOR = 250;
    
    private static final int MAX_SPEED_FACTOR = 300;
    
    private static final int MIN_SPEED_FACTOR = 10;
    
    private static final int NUM_SLOTS = 10;
    
    private final Activity mActivity;
    
    private final String mManualSaveDir;
    
    private final String mAutoSaveFile;
    
    private MenuItem mSlotMenuItem;
    
    private MenuItem mGameSpeedItem;
    
    private Menu mSlotSubMenu;
    
    private AppData mAppData;
    
    private List<Integer> mIgnoredKeyCodes;
    
    private int mSlot = 0;
    
    private boolean mCustomSpeed = false;
    
    private int mSpeedFactor = DEFAULT_SPEED_FACTOR;
    
    public GameMenuHandler( Activity activity, String manualSaveDir, String autoSaveFile )
    {
        mActivity = activity;
        mManualSaveDir = manualSaveDir;
        mAutoSaveFile = autoSaveFile;
    }
    
    public void onCreateOptionsMenu( Menu menu )
    {
        // Inflate the in-game menu, record the dynamic menu items/submenus for later
        mActivity.getMenuInflater().inflate( R.menu.game_activity, menu );
        mSlotMenuItem = menu.findItem( R.id.ingameSlot );
        mSlotSubMenu = mSlotMenuItem.getSubMenu();
        mGameSpeedItem = menu.findItem( R.id.ingameSetSpeed );
        mGameSpeedItem.setTitle( mActivity.getString( R.string.ingameSetSpeed_title,
                BASELINE_SPEED_FACTOR ) );
        
        // Get the app data after the activity has been created
        mAppData = new AppData( mActivity );
        
        // Initialize to the last slot used
        setSlot( mAppData.getLastSlot(), false );
        
        // Initialize the multiplayer settings menu
        UserPrefs userPrefs = new UserPrefs( mActivity );
        mIgnoredKeyCodes = userPrefs.unmappableKeyCodes;
        menu.findItem( R.id.ingameHardwareMap ).setVisible( userPrefs.isMultiplayer );
        menu.findItem( R.id.ingameHardwareMap1 ).setVisible( userPrefs.inputMap1.isEnabled() );
        menu.findItem( R.id.ingameHardwareMap2 ).setVisible( userPrefs.inputMap2.isEnabled() );
        menu.findItem( R.id.ingameHardwareMap3 ).setVisible( userPrefs.inputMap3.isEnabled() );
        menu.findItem( R.id.ingameHardwareMap4 ).setVisible( userPrefs.inputMap4.isEnabled() );
    }
    
    public void onOptionsItemSelected( MenuItem item )
    {
        switch( item.getItemId() )
        {
            case R.id.slot0:
                setSlot( 0, true );
                break;
            case R.id.slot1:
                setSlot( 1, true );
                break;
            case R.id.slot2:
                setSlot( 2, true );
                break;
            case R.id.slot3:
                setSlot( 3, true );
                break;
            case R.id.slot4:
                setSlot( 4, true );
                break;
            case R.id.slot5:
                setSlot( 5, true );
                break;
            case R.id.slot6:
                setSlot( 6, true );
                break;
            case R.id.slot7:
                setSlot( 7, true );
                break;
            case R.id.slot8:
                setSlot( 8, true );
                break;
            case R.id.slot9:
                setSlot( 9, true );
                break;
            case R.id.ingameQuicksave:
                saveSlot();
                break;
            case R.id.ingameQuickload:
                loadSlot();
                break;
            case R.id.ingameSetSpeed:
                toggleSpeed();
                break;
            case R.id.ingameSave:
                saveStateFromPrompt();
                break;
            case R.id.ingameLoad:
                loadStateFromPrompt();
                break;
            case R.id.ingameSpeed:
                configureSpeed();
                break;
            case R.id.ingameReset:
                resetState();
                break;
            case R.id.ingameHardwareMap1:
                HardwareMap.Prompter.show( mActivity, 1, mIgnoredKeyCodes );
                break;
            case R.id.ingameHardwareMap2:
                HardwareMap.Prompter.show( mActivity, 2, mIgnoredKeyCodes );
                break;
            case R.id.ingameHardwareMap3:
                HardwareMap.Prompter.show( mActivity, 3, mIgnoredKeyCodes );
                break;
            case R.id.ingameHardwareMap4:
                HardwareMap.Prompter.show( mActivity, 4, mIgnoredKeyCodes );
                break;
            case R.id.ingameMenu:
                quitToMenu();
                break;
            default:
                break;
        }
    }
    
    private void toggleSpeed()
    {
        mCustomSpeed = !mCustomSpeed;
        int speed = mCustomSpeed ? mSpeedFactor : BASELINE_SPEED_FACTOR;
        
        NativeMethods.stateSetSpeed( speed );
        mGameSpeedItem.setTitle( mActivity.getString( R.string.ingameSetSpeed_title, speed ) );
    }
    
    private void setSlot( int value, boolean notify )
    {
        // Sanity check and persist the value
        mSlot = value % NUM_SLOTS;
        mAppData.setLastSlot( mSlot );
        
        // Set the slot in the core
        NativeMethods.stateSetSlotEmulator( mSlot );
        
        // Refresh the slot item in the top-level options menu
        if( mSlotMenuItem != null )
            mSlotMenuItem.setTitle( mActivity.getString( R.string.ingameSlot_title, mSlot ) );
        
        // Refresh the slot submenu
        if( mSlotSubMenu != null )
        {
            MenuItem item = mSlotSubMenu.getItem( mSlot );
            if( item != null )
                item.setChecked( true );
        }
        
        // Send a toast if requested
        if( notify )
            Notifier.showToast( mActivity, R.string.toast_savegameSlot, mSlot );
    }
    
    private void saveSlot()
    {
        Notifier.showToast( mActivity, R.string.toast_savingSlot, mSlot );
        NativeMethods.stateSaveEmulator();
    }
    
    private void loadSlot()
    {
        Notifier.showToast( mActivity, R.string.toast_loadingSlot, mSlot );
        NativeMethods.stateLoadEmulator();
    }
    
    private void saveStateFromPrompt()
    {
        NativeMethods.pauseEmulator();
        CharSequence title = mActivity.getText( R.string.ingameSave_title );
        CharSequence hint = mActivity.getText( R.string.gameMenu_saveHint );
        int inputType = InputType.TYPE_CLASS_TEXT;
        Prompt.promptText( mActivity, title, null, hint, inputType, new OnTextListener()
        {
            @Override
            public void onText( CharSequence text, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                    saveState( text.toString() );
                NativeMethods.resumeEmulator();
            }
        } );
    }
    
    private void loadStateFromPrompt()
    {
        NativeMethods.pauseEmulator();
        CharSequence title = mActivity.getText( R.string.ingameLoad_title );
        File startPath = new File( mManualSaveDir );
        Prompt.promptFile( mActivity, title, null, startPath, new OnFileListener()
        {
            @Override
            public void onFile( File file, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                    loadState( file );
                NativeMethods.resumeEmulator();
            }
        } );
    }
    
    private void saveState( final String filename )
    {
        final File file = new File( mManualSaveDir + "/" + filename );
        if( file.exists() )
        {
            String title = mActivity.getString( R.string._confirmation );
            String message = mActivity.getString( R.string.gameMenu_confirmFile, filename );
            Prompt.promptConfirm( mActivity, title, message, new OnClickListener()
            {
                @Override
                public void onClick( DialogInterface dialog, int which )
                {
                    if( which == DialogInterface.BUTTON_POSITIVE )
                    {
                        Notifier.showToast( mActivity, R.string.toast_overwritingGame,
                                file.getName() );
                        NativeMethods.fileSaveEmulator( file.getAbsolutePath() );
                    }
                }
            } );
        }
        else
        {
            Notifier.showToast( mActivity, R.string.toast_savingGame, file.getName() );
            NativeMethods.fileSaveEmulator( file.getAbsolutePath() );
        }
    }
    
    private void loadState( File file )
    {
        Notifier.showToast( mActivity, R.string.toast_loadingGame, file.getName() );
        NativeMethods.fileLoadEmulator( file.getAbsolutePath() );
    }
    
    private void resetState()
    {
        NativeMethods.pauseEmulator();
        CharSequence title = mActivity.getText( R.string._confirmation );
        CharSequence message = mActivity.getText( R.string.gameMenu_confirmReset );
        Prompt.promptConfirm( mActivity, title, message, new OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                {
                    Notifier.showToast( mActivity, R.string.toast_resettingGame );
                    NativeMethods.resetEmulator();
                }
                NativeMethods.resumeEmulator();
            }
        } );
    }
    
    private void configureSpeed()
    {
        NativeMethods.pauseEmulator();
        CharSequence title = mActivity.getText( R.string.ingameSpeed_title );
        CharSequence hint = mActivity.getText( R.string.gameMenu_speedHint );
        int inputType = InputType.TYPE_CLASS_NUMBER;
        Prompt.promptText( mActivity, title, null, hint, inputType, new OnTextListener()
        {
            @Override
            public void onText( CharSequence text, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                {
                    if( text.length() != 0 )
                    {
                        mSpeedFactor = SafeMethods.toInt( text.toString(), DEFAULT_SPEED_FACTOR );
                        mSpeedFactor = Utility.clamp( mSpeedFactor, MIN_SPEED_FACTOR,
                                MAX_SPEED_FACTOR );
                        mCustomSpeed = true;
                        NativeMethods.stateSetSpeed( mSpeedFactor );
                        mGameSpeedItem.setTitle( mActivity.getString(
                                R.string.ingameSetSpeed_title, mSpeedFactor ) );
                    }
                }
                NativeMethods.resumeEmulator();
            }
        } );
    }
    
    private void quitToMenu()
    {
        // Return to previous activity (MenuActivity)
        // It's easier just to finish so that everything will be reloaded next time
        // mActivity.finish();
        
        // TODO: Uncomment the line above and delete the block below
        
        // ////
        // paulscode: temporary workaround for ASDP bug after emulator shuts down
        Notifier.showToast( mActivity, R.string.toast_savingSession );
        CoreInterface.setOnEmuStateChangeListener( new OnEmuStateChangeListener()
        {
            @Override
            public void onEmuStateChange( int newState )
            {
                if( newState == CoreInterface.EMULATOR_STATE_RUNNING )
                {
                    System.exit( 0 ); // Bad, bad..
                    CoreInterface.setOnEmuStateChangeListener( null );
                    mActivity.finish();
                }
            }
        } );
        NativeMethods.fileSaveEmulator( mAutoSaveFile );
        // ////
    }
}
