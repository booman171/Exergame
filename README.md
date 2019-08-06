# Exergame

This Android app uses an wireless IMU (motion tracker) and ECG (brainwave sensor) as input, via bluetooth, in to a mobile game. Using the IMU, the player's physical motion to control the ship is ran through a trained Tensorflow model to infer which exercise motion is being performed. The trained model recognizes 5 activities, including 4 upper body exercises and rest. The ECG sensor is used to replensih the shields of the player's ship by using the calmness algorithm to break a set threshold.

## Devices
The app connects to the MetaMotionR or MetaMotionC wearable device from mbientlab and the Mindwave Mobile from NeuroSky.

https://mbientlab.com/metamotionc/

https://store.neurosky.com/pages/mindwave


## Building blocks

This app builds upon several projects, tutorials, and applications listed below:

- [mbientlab Android Tutorial] (https://github.com/mbientlab/MetaWear-Tutorial-Android)

- [Human Activity Recognition with Tensorflow and Android] (https://github.com/curiousily/TensorFlow-on-Android-for-Human-Activity-Recognition-with-LSTMs)

- [Dodging Android Game] (https://github.com/jcpdas9/dodge-android)

- [Log MindWave Mobile data] (https://github.com/terminationshok/mindwavelogger)
