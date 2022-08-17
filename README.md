<h1>CapacitorJS plugin for Solana dApps</h1>  

  <p>My goal with this project is to help drive the adoption of Solana SDKs across mobile & web. By providing an open source CapacitorJS plugin based on the existing Solana SDKs the idea is make it easier for more developers to build cross-platform dApps.<p>
    
  <p>CapacitorJS is part of the Ionic ecosysystem and has a large and active community of developers. Capacitor/Ionic is found in approximately 10% of all Apps on Google Play and the App Store and averages around 200,000 weekly downloads on npm.</p>
  
  <p>By offering a Capacitor plugin we make it easier for Capacitor/Ionic community to start building on Solana.</p>
  
  <h3>What is Capacitor?</h3>
  <p>Capacitor is a cross-platform native runtime for web apps. Capacitor plugins provide a bridge layer, which allows web developers building cross-platform apps to access native functionality from a single API.<p>
  <p>A detailed description and documentation can be found at https://capacitorjs.com/.<p>
  <p>The examples in this repo are built with Angular and Ionic Framework UI components but it should be noted that CapacitorJS is completely framework agnostic.</p>
  <h3>Current State</h3>
  <p>The plugin was built as a proof-of-concept in just over a week at the Singapore Solana Summer Camp 2022. It is <b>definitely not</b> ready for production use.</p>
  <p>The plugin is a wrapper around the Kotlin "fake dApp" Android application build by the core Solana Mobile team (see https://github.com/solana-mobile/mobile-wallet-adapter/tree/main/android/fakedapp). It allows a developer building a web app to access to all the methods in MainViewModel.kt in the underlying Kotlin project.</p>
  <p>Wallet detection functionality has also been added. The plugin is able to detect which apps are installed and available in the environment in which it is running (i.e. web browser or Android). The current implementation for Android relies on manifest permissions settings that may require prior agreement from Play Store moderators.</p>
  <h4>Demo</h4>
  <p>The code is this repo is the Capacitor source code for the plugin.<p>
  <p>There is a companion repo here () which contains an Angular front end which can be deployed to the web and Android devices using Android studio and the Ionic/Capacitor CLI. Full instructions are in the readme for that repo.<p>
  
  <h3>Next steps</h3>
  <h4>Overall direction</h4>
  <p>I started this project because I wanted to create a Summer Camp submission that had the potential to really grow the number of developers working in the ecosystem. Solana is already known for the strength of it's developer community so my hope is that there will be appetite to participate in and support this project. If there is I would propose to run this project on an open source community-backed basis with a permissive license e.g. MIT/Apache2.</p>
  <p>Over the course of the Summer Camp I was also in touch with Max Lynch who is CEO and founder of Ionic/Capacitor about this project. Whilst I do not want to speak for him here, I have seen Max and his team go out of their way to support the CapacitorJS community. I am sure the Ionic folks would be able to help promote a Solana plugin to the community if we can create something production ready.</p>
  <h4>Project design</h4>
  <p>Through the Summer Camp I focused on creating a working POC focused on the new Android SDK for the 'fakedapp' provided by the core team. In the long run I see the case for two plugins, one for dApps and another for wallet projects. This would depend on the Solana Mobile team's roadmap and plans for the core libraries.</p>
  <h4>Code / existing implementation</h4>
  <p>Because the focus was on allowing web developers to access the 'fakedapp' native functionality, some more thought is required around how the 'fakedapp' functionality maps to the functionality provided by the web wallet adaptor.</p>
  <p>In it's current form the demo application provides a single function for developers to call get available wallets for a given environment. The next step will be to map (where possible) the remaining 'fakedapp' functionality to the web SDK the define an appropriate API/interfaces. The Angular demo app which was written in one sitting also needs to be cleaned up to make it easier to see what is happening.</p>
  <p>I am not a Kotlin developer so I am sure there is room for improvement within the Android implementation. Currently the approach to managing state in the app is to push data to the web layer, which I suspect is not ideal. Results from the co-routines are returned to the web layer by passing through the plugin calls to the Kotlin functions and then resolving them at the right point in the code.</p>
  <p>No serious thought has been given to securing the plugin yet, nor has it been extensively tested.</p>
