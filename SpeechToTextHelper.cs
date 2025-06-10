using System;
using System.Globalization;
using System.Speech.Recognition;

namespace SpeechHelper
{
    class Program
    {
        static void Main(string[] args)
        {
            try
            {
                // Create an in-process speech recognizer with a specific culture (en-IN).
                // You can change the culture string as needed.
                using (SpeechRecognitionEngine recognizer = new SpeechRecognitionEngine(new CultureInfo("en-IN")))
                {
                    // Create and load a dictation grammar.
                    recognizer.LoadGrammar(new DictationGrammar());

                    // Subscribe to events.
                    recognizer.SpeechRecognized += new EventHandler<SpeechRecognizedEventArgs>(recognizer_SpeechRecognized);
                    recognizer.SpeechRecognitionRejected += new EventHandler<SpeechRecognitionRejectedEventArgs>(recognizer_SpeechRecognitionRejected);

                    // Configure the recognizer to use the default audio device.
                    try
                    {
                        recognizer.SetInputToDefaultAudioDevice();
                    }
                    catch (InvalidOperationException ex)
                    {
                        Console.WriteLine("No audio input device found: " + ex.Message);
                        return;
                    }

                    // Start asynchronous, continuous recognition.
                    recognizer.RecognizeAsync(RecognizeMode.Multiple);

                    // Keep the console application running.
                    Console.WriteLine("Speech helper started. Listening for speech...");
                    Console.ReadLine();
                }
            }
            catch (Exception ex)
            {
                // Write any errors to the console.
                Console.WriteLine("An error occurred in the speech helper: " + ex.Message);
            }
        }

        // Handle the SpeechRecognized event.
        private static void recognizer_SpeechRecognized(object sender, SpeechRecognizedEventArgs e)
        {
            if (e.Result != null && !string.IsNullOrEmpty(e.Result.Text))
            {
                Console.WriteLine(e.Result.Text);
                Console.Out.Flush();
            }
        }

        // Handle the SpeechRecognitionRejected event.
        private static void recognizer_SpeechRecognitionRejected(object sender, SpeechRecognitionRejectedEventArgs e)
        {
            Console.WriteLine("Speech could not be recognized.");
            Console.Out.Flush();
        }
    }
}
