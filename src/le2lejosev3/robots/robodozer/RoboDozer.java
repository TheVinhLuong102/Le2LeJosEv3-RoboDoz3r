/**
 * 
 */
package le2lejosev3.robots.robodozer;

import java.util.logging.Logger;

import le2lejosev3.logging.Setup;
import le2lejosev3.pblocks.Display;
import le2lejosev3.pblocks.InfraredSensor;
import le2lejosev3.pblocks.MediumMotor;
import le2lejosev3.pblocks.MoveTank;
import le2lejosev3.pblocks.Sound;
import le2lejosev3.pblocks.TouchSensor;
import le2lejosev3.pblocks.Wait;
import lejos.hardware.Button;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;

/**
 * RoboDoz3r.
 * 
 * @author Roland Blochberger
 */
public class RoboDozer {

	private static Class<?> clazz = RoboDozer.class;
	private static final Logger log = Logger.getLogger(clazz.getName());

	// the robot configuration
	static Port motorPortA = MotorPort.A;
	static Port motorPortB = MotorPort.B;
	static Port motorPortC = MotorPort.C;
	static Port touchSensorPort = SensorPort.S1;
	static Port infraredSensorPort = SensorPort.S4;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// setup logging to file
		Setup.log2File(clazz);
		log.fine("Starting ...");

		// Display text "RoboDoz3r" on grid coordinates 2, 2 in black (false) large font
		// (2) and clear screen before
		Display.textGrid("RoboDoz3r", true, 2, 2, Display.COLOR_BLACK, Display.FONT_LARGE);

		// instantiate and run the wait for exit thread
		Thread waitForExit = new WaitForExit();
		waitForExit.start();

		// instantiate and run the main thread
		Thread mainThread = new MainThread();
		mainThread.start();
	}

}

/**
 * The main program thread.
 */
class MainThread extends Thread {
	private static final Logger log = Logger.getLogger(MainThread.class.getName());

	// the motors
	private final MediumMotor motorA;
	private final MoveTank move;

	// the sensors
	private final TouchSensor touch;
	private final InfraredSensor infra;

	/**
	 * Constructor
	 */
	public MainThread() {

		// instantiate the motors
		motorA = new MediumMotor(RoboDozer.motorPortA);
		move = new MoveTank(RoboDozer.motorPortB, RoboDozer.motorPortC);

		// instantiate the sensors
		touch = new TouchSensor(RoboDozer.touchSensorPort);
		infra = new InfraredSensor(RoboDozer.infraredSensorPort);
	}

	/**
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		log.fine("");

		// Play sound file "Motor start" with volume 56 and wait until done (0)
		Sound.playFile("Motor start", 56, Sound.WAIT);

		// let the engine sound idle for 2 seconds
		for (long st = System.currentTimeMillis(); System.currentTimeMillis() - st < 2000L;) {
			// Play sound file "Motor idle" with volume 56 and wait until done (0)
			Sound.playFile("Motor start", 56, Sound.WAIT);
		}

		// main loop
		while (Button.ESCAPE.isUp()) {

			// Driving mode
			drivingMode();

			// Play sound file "Airbrake" with volume 100 and wait until done (0)
			Sound.playFile("Airbrake", 100, Sound.WAIT);

			// Auto mode
			autoMode();

			// Play sound file "Airbrake" with volume 100 and wait until done (0)
			Sound.playFile("Airbrake", 100, Sound.WAIT);
		}

		log.fine("end");
	}

	/**
	 * Driving mode
	 */
	private void drivingMode() {
		log.fine("");
		// run in driving mode while touch sensor not pressed
		do {
			// Handle raise or lower the shovel
			raiseLower();

			// Handle remote drive
			remoteDrive();

		} while (touch.compareState(TouchSensor.RELEASED));
	}

	/**
	 * Handle raise or lower the shovel
	 */
	private void raiseLower() {
		// get remote control command from channel 4
		int command = infra.measureRemote(4);

		switch (command) {
		case InfraredSensor.TOP_LEFT: // command button 1
			// switch motor A on with power 10
			motorA.motorOn(10);
			break;

		case InfraredSensor.BOTTOM_LEFT: // command button 2
			// switch motor A on with power -10
			motorA.motorOn(-10);
			break;

		default:
			// switch motor A off
			motorA.motorOff(true);
			break;
		}

	}

	/**
	 * Handle remote drive
	 */
	private void remoteDrive() {
		// get remote control command from channel 1
		int command = infra.measureRemote(1);

		switch (command) {
		case InfraredSensor.TOP_LEFT: // command button 1
			// Move tank on with power left 0 and power right -50
			move.motorsOn(0, -50);
			break;

		case InfraredSensor.BOTTOM_LEFT: // command button 2
			// Move tank on with power left 0 and power right 50
			move.motorsOn(0, 50);
			break;

		case InfraredSensor.TOP_RIGHT: // command button 3
			// Move tank on with power left -50 and power right 0
			move.motorsOn(-50, 0);
			break;

		case InfraredSensor.BOTTOM_RIGHT: // command button 4
			// Move tank on with power left 50 and power right 0
			move.motorsOn(50, 0);
			break;

		case InfraredSensor.TOP_BOTH: // command button 5
			// Move tank on with power left -50 and power right -50
			move.motorsOn(-50, -50);
			break;

		case InfraredSensor.TOP_LEFT_BOTTOM_RIGHT: // command button 6
			// Move tank on with power left 50 and power right -50
			move.motorsOn(50, -50);
			break;

		case InfraredSensor.TOP_RIGHT_BOTTOM_LEFT: // command button 7
			// Move tank on with power left -50 and power right 50
			move.motorsOn(-50, 50);
			break;

		case InfraredSensor.BOTTOM_BOTH: // command button 5
			// Move tank on with power left 50 and power right 50
			move.motorsOn(50, 50);
			break;

		default: // command 0
			// Move tank off with brake
			move.motorsOff(true);
			break;
		}
	}

	/**
	 * Auto mode
	 */
	private void autoMode() {
		log.fine("");
		// run in auto mode while touch sensor not pressed
		float prox;
		do {
			// measure infrared proximity
			prox = infra.measureProximity();
			// compare proximity
			if (prox < 50) {
				// obstacle:
				// Move tank off with brake
				move.motorsOff(true);

				// Wait 1 second
				Wait.time(1F);
				// Move tank on for 1 second with power left 30 and power right 30 then brake
				move.motorsOnForSeconds(30, 30, 1F, true);

				// Move tank on for 1 second with power left 50 and power right -50 then brake
				move.motorsOnForSeconds(50, -50, 1F, true);

			} else {
				// no obstacle:
				// Move tank on with power left -50 and power right -50
				move.motorsOn(-50, -50);
			}

		} while (touch.compareState(TouchSensor.RELEASED));
	}
}

/**
 * Background loop to detect the center (enter) button pressed on the EV3.
 */
class WaitForExit extends Thread {
	private static final Logger log = Logger.getLogger(WaitForExit.class.getName());

	/**
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		log.fine("");
		// wait until enter button is pressed
		while (Button.ENTER.isUp()) {
			// let other threads run
			Thread.yield();
			// wait 2 milliseconds until next check (necessary?)
			Wait.time(0.002F);
		}
		log.fine("Exit");
		// exit the program (normally)
		System.exit(0);
	}
}
