# PPGbetter

Better photoplethysmograph from a mobile camera – Android app.

PPGbetter is an Android application for measuring heart rate using photoplethysmography (PPG) with a regular smartphone camera and LED flash. It was developed as a data acquisition tool for a research project on heart rate variability (HRV) and AI-based peak detection.

> This repository contains the Android acquisition side of the project.  
> The Python code for signal processing and machine-learning-based analysis is available at:  
> **https://github.com/JanBancerewicz/research-project**

## Research context and contributors

This repository originates from a research project carried out at Gdańsk University of Technology (Politechnika Gdańska).  
The work is described in the following manuscript submitted to TASK Quarterly:

> Jan Kosma Bancerewicz, Julian Jerzy Kotłowski, Ostap Lozovyy,  
> Julia Beata Morawska, Mateusz Rzęsa,  
> **“Analysis of heart rate variability using mobile devices and machine learning”**,  
> TASK Quarterly, 2025 (manuscript in review).  
> PDF: [Analysis_of_heart_rate_variability_using_mobile_devices_and_machine_learning.pdf](https://github.com/JanBancerewicz/research-project/blob/doc/Analysis_of_heart_rate_variability_using_mobile_devices_and_machine_learning%20(compiled).pdf)


The project and this codebase were developed collaboratively by:

- **Jan Kosma Bancerewicz** – (https://github.com/JanBancerewicz)
- **Julian Jerzy Kotłowski** – (https://github.com/julkot1)
- **Ostap Lozovyy** – (https://github.com/agoneiro)
- **Julia Beata Morawska** – (https://github.com/jmorawska03)   
- **Mateusz Rzęsa** – (https://github.com/L1itrer)

If you use this repository or build upon this work, please reference the above publication (see also the [Citation](#citation) section).


---

## Overview

The app measures the subtle changes in light transmitted through a finger placed on the phone’s rear camera. These changes correspond to variations in blood volume in the microvascular bed of the skin and form a photoplethysmographic signal.

PPGbetter:

- captures the signal using the rear camera and flashlight,
- displays the PPG waveform in real time,
- estimates heart rate (BPM) from detected peaks,
- lets the user mark inhalation / exhalation events,
- saves the full recording (signal + markers) for offline analysis.

The recorded data was used in the companion research repository:

- **HRV & AI analysis (Python):**  
  https://github.com/JanBancerewicz/research-project  

The corresponding paper is published in TASK Quarterly (2025).

### Visualization

<p align="center">
  <img src="https://github.com/user-attachments/assets/2480a2f7-b0ac-4e8d-8412-3b42f78c8079"
       alt="PPGbetter main screen with live PPG waveform and controls"
       width="400" />
</p>

*Main screen of PPGbetter: live PPG waveform, current heart rate and controls. The red camera view resembles data acquisition from a tip of a finger.*


---

## How it works

1. **Acquisition**

   - The user launches the app and gently places a fingertip on the rear camera.
   - The LED flash automatically turns on, illuminating the skin.
   - The camera records video frames where the intensity of reflected light changes with each heartbeat.

2. **PPG signal extraction**

   - For each frame the app extracts the **green channel**, which best reflects blood flow changes.
   - It computes the **average brightness** of all pixels in that channel.
   - These per-frame averages form a 1D time series: the raw PPG signal.

3. **Filtering and denoising**

   - The raw PPG signal is passed through a **low-pass filter** to suppress high-frequency noise and small fluctuations unrelated to pulsation.
   - This produces a smoother waveform that still follows the dominant cardiac rhythm.

4. **Peak detection and heart rate**

   - Local maxima of the filtered signal are detected as candidate heart beats.
   - Time differences between consecutive peaks are computed.
   - Instantaneous heart rate (BPM) is estimated, e.g.  
     \[
       \text{BPM} = \frac{60}{\overline{\Delta t}} ,
     \]
     where \(\overline{\Delta t}\) is the average time between peaks in seconds.


5. **Data logging**

   - During a measurement the app continuously logs:
     - timestamped PPG samples,
     - estimated heart rate,
   - At the end of the session these data are saved to a file (e.g. CSV-like format) for offline processing.

---

## Features

- Real-time visualization of the PPG waveform.
- Continuous heart rate estimation in beats per minute (BPM).
- Manual annotation of breathing phases (inhale / exhale). #REMOVED
- Export of measurement sessions for further analysis (e.g. in Python / MATLAB).
- Runs on any Android phone with a rear camera and LED flash.
- Open-source under the GPL-2.0 license.

---

## Building and running

### Requirements

- Android Studio (current stable version).
- Android device with:
  - rear camera,
  - LED flash,
  - Android version compatible with the project Gradle configuration (see `build.gradle.kts` in the repo).
- USB debugging enabled on the device (or an emulator, though real hardware is recommended).

### Steps

1. Clone the repository:

   ```bash
   git clone https://github.com/JanBancerewicz/PPGbetter.git
   cd PPGbetter
   ```

2. Open the project in Android Studio (File → Open…, select the project directory).

3. Let Gradle sync and resolve dependencies.

4. Connect an Android device via USB and enable USB debugging.

5. Select the desired run configuration and click "Run" in Android Studio.

6. Once the app is installed, launch it on the device.

---

## Using the app

1. Start **PPGbetter** on your phone.
2. Gently place your fingertip over the rear camera and flash.
3. Wait a few seconds until:
   - the waveform stabilizes,
   - the heart rate (BPM) starts updating.
4. When finished:
   - stop the recording so that the data file is written to storage,
   - copy the recorded file to your computer if you plan to analyse it further (e.g. with the research Python code in the companion repository).

---

## Integration with the HRV research project

PPGbetter was designed as the mobile front-end for a broader research pipeline for heart rate variability analysis:

- **Research code (ECG/PPG, AI peak detectors, HRV & PTT analysis):**  
  https://github.com/JanBancerewicz/research-project

In that project:

- PPGbetter recordings are used as the **PPG input**,  
- ECG is recorded using a **Polar H10** chest strap,  
- Python scripts:
  - filter signals,
  - run classical and neural peak detectors,
  - compute:
    - HRV indicators (SDNN, RMSSD, etc.),
    - pulse transit time (PTT),
    - comparisons between ECG- and PPG-based metrics.

PPGbetter therefore serves as a practical demonstration that a commodity smartphone can supply PPG signals of sufficient quality for HRV-oriented research when processed appropriately.

---

## Comparison with Polar ECG measurements

To assess the quality of PPG-based heart rate estimation, measurements from PPGbetter were compared with heart rate derived from Polar ECG data (processed by an AI-based R-peak detector).

### First measurement

<p align="center">
  <img src="https://github.com/user-attachments/assets/5c8a0faa-e583-48bc-b7ac-819e85a7022a"
       alt="Heart rate comparison: PPGbetter vs Polar ECG (dynamic segment)"
       height="400" />
</p>

In this more dynamic segment, the HR curve from the mobile app is smoother and oscillates in a narrow range around 80–85 bpm.  
The ECG-based reference shows higher short-term variability, with values roughly between 75 and 100 bpm. This reflects the fact that ECG responds more directly to rapid physiological changes but is also more sensitive to artifacts. The visible increase in heart rate around 40–60 seconds may correspond to a brief physical effort or a change of posture.

### Second measurement

<p align="center">
  <img src="https://github.com/user-attachments/assets/bd6179b1-4373-4dbb-bd0c-010c1f069eec"
       alt="Heart rate comparison: PPGbetter vs Polar ECG (rest segment)"
       height="400" />
</p>

In a more stable, resting segment, both methods produce very similar heart rate trajectories.  
Values remain in a tight band around 80–85 bpm, with the ECG-based curve still showing slightly more variability. This suggests that, under calm conditions, PPGbetter can approximate the ECG-derived heart rate reasonably well, while ECG remains preferable for capturing very short-term fluctuations.

### Observations

**PPGbetter:**

- produces a smoother and more stable HR trace,  
- is less sensitive to short, transient changes,  
- is convenient and non-invasive (just a phone).

**ECG + AI peak detection:**

- better captures rapid changes in heart rate,  
- can be more affected by noise and movement artifacts,  
- remains the gold standard when high temporal precision is required.

For general monitoring and exploratory HR/HRV analysis, the PPG-based approach is often sufficient; for precise clinical-grade analysis, ECG is still more appropriate.

---

## Citation

If you use this code or ideas in your research, please cite:

> J. K. Bancerewicz, J. J. Kotłowski, O. Lozovyy, J. B. Morawska, M. Rzęsa,  
> **“Analysis of heart rate variability using mobile devices and machine learning”**,  
> TASK Quarterly, 2025 (in review).  #TODO fix after review
> PDF: [Analysis_of_heart_rate_variability_using_mobile_devices_and_machine_learning.pdf](https://github.com/JanBancerewicz/research-project/blob/doc/Analysis_of_heart_rate_variability_using_mobile_devices_and_machine_learning%20(compiled).pdf)

---

## License

This project is released under the **MIT License**.  
For full details, see the [`LICENSE`](./LICENSE) file in this repository.

---

## Disclaimer

PPGbetter is intended **for research and educational purposes only** and is **not** a medical device.  
It must not be used for diagnosis, treatment, or monitoring of medical conditions without appropriate certification and clinical validation.
