<img src="https://github.com/nolsonlabs/solana-capacitor-dapp/blob/main/Solana-Mobile-x-Capacitor.png?raw=true">.

<h1>Capacitor plugin for Solana dApps</h1>

  <h3>TLDR;</h3>
  <p>Install the plugin for use in any web app: "npm i solana-wallet-adaptor-capacitor". Installation notes are <a href="https://github.com/nolsonlabs/solana-capacitor-dapp-demo/blob/main/README.md">here.</a></p>

  <p>API notes are <a href="https://github.com/nolsonlabs/solana-capacitor-dapp/blob/main/API.md">here</a>.</p>

  <p>Web deployment of demo app: https://solana-mobile-capacitor.web.app/</p>

  <p>Wallet plugin available <a href="https://github.com/nolsonlabs/solana-capacitor-wallet">here</a>.

  <p>Current version was built to illustrate the concept in just over a week at the Singapore Solana Summer Camp 2022. <b>It is definitely not ready for production use.</b></p>

  <p>Free and open source. Comments, questions, feedback and contributions most welcome.</p>

  <p><b>Demo video on Twitter</b></p>

  <blockquote class="twitter-tweet"><p lang="en" dir="ltr">📺<a href="https://twitter.com/solana?ref_src=twsrc%5Etfw">@solana</a> x <a href="https://twitter.com/capacitorjs?ref_src=twsrc%5Etfw">@capacitorjs</a> demo<br>🛠️Build web apps on <a href="https://twitter.com/solanamobile?ref_src=twsrc%5Etfw">@solanamobile</a> native SDKs using your favourite Javascript framework! <a href="https://t.co/pPAe7K6X9W">pic.twitter.com/pPAe7K6X9W</a></p>&mdash; Solana x Capacitor (@solanacapacitor) <a href="https://twitter.com/solanacapacitor/status/1568079874937733120?ref_src=twsrc%5Etfw">September 9, 2022</a></blockquote>

  <h3>What is this project?</h3>
  <p>This project exists to help drive the adoption of Solana SDKs across mobile & web. The idea is make it easier for more developers to build cross-platform dApps by providing an open source Capacitor plugin based on the existing Solana SDKs.<p>
    
  <p>Capacitor is part of the Ionic ecosysystem and has a large and active community of developers. Capacitor/Ionic is found in approximately 10% of all Apps on Google Play and the App Store and averages around 200,000 weekly downloads on npm.</p>
  
  <p>By offering a Capacitor plugin we make it easier for Capacitor/Ionic community to start building on Solana.</p>
  
  <h3>What is Capacitor?</h3>
  <p>Capacitor is a cross-platform native runtime for web apps. Capacitor plugins provide a bridge layer, which allows web developers building cross-platform apps to access native functionality from a single API.<p>
  <p>A detailed description and documentation can be found at https://capacitorjs.com/.<p>

  <h3>Current State</h3>
  <p>The plugin was built as a to illustrate the concept in just over a week at the Singapore Solana Summer Camp 2022. <b>It is definitely not ready for production use.</b></p>
  <p>The plugin is a wrapper around the Kotlin "fakedapp" Android application build by the Solana Mobile team (see https://github.com/solana-mobile/mobile-wallet-adapter/tree/main/android/fakedapp). It allows a developer building a web app to access to all the methods in MainViewModel.kt in the underlying Kotlin project.</p>
  <p>Wallet detection functionality has also been added. The plugin is able to detect which apps are installed and available in the environment in which it is running (i.e. web browser or Android).</p>
  <p>The current implementation for Android relies on manifest permissions settings that may require prior agreement from moderators for Play Store distribution.</p>
  <p>This functionlity is only currently only implemented for wallets that offer transaction / deeplink functionality.</p>
  <h4>Demo & Installation</h4>
  <p>The code is this repo is the Capacitor source code for the plugin.</p>
  <p>There is a <a href="https://github.com/nolsonlabs/solana-capacitor-dapp-demo">companion repo</a> which contains a demo Angular app. This can be deployed to the web and Android devices using Android studio and the Ionic/Capacitor CLI. Usage instructions are in the demo repo readme.<p>

  <p>A short presentation given at the Singapore Summer Camp is <a href="https://docs.google.com/presentation/d/18OUGsrpjco8OxIglSzq0gu4s_Fqaz9uB/edit#slide=id.p1">here</a>. It contains a link to an even shorter demo video.</p>
  
  <h3>Next steps</h3>
  <h4>Overall direction</h4>
  <p>I propose to run this project on an open source community-backed basis with a permissive license e.g. MIT/Apache2. Solana is already known for the strength of it's developer community so my hope is that there will be appetite to participate in this project.</p>
  <p>Over the years I have seen the Ionic & Capacitor team go out of their way to support the Capacitor community. I don't speak for them here but I am sure the Ionic folks would be able to help promote a Solana plugin to the community if we can create something production ready.</p>
  <h4>Project design</h4>
  <p>Through the Summer Camp I focused on creating a working POC focused on the new Android SDK for the 'fakedapp' provided by the core team. In the long run I see a case for two plugins, one for dApps and another for wallet projects. This would depend on the Solana Mobile team's roadmap and plans for the core libraries.</p>

  <h4>Code / existing implementation</h4>
  <p>Because the focus was on allowing web developers to access the 'fakedapp' native functionality, some more thought is required around how the 'fakedapp' functionality maps to the functionality provided by the web wallet adaptor.</p>
  <p>The demo application fully replicates the 'fakedapp' app. Additionally it allows developers get available wallets for any given environment. The next step will be to map 'fakedapp' functionality to the web SDKs the define an appropriate API/interfaces.</p>
  <p>The Angular demo app which was written in one sitting also needs to be cleaned up to make it easier to see what is happening.</p>
  <p>I am not a Kotlin developer so I am sure there is room for improvement within the Android implementation. Currently the approach to managing state in the app is to push data to the web layer, which I suspect is not ideal. Results from the co-routines are returned to the web layer by inserting the plugin calls to the Kotlin functions and then resolving them at the right point in the code.</p>
  <p>No serious thought has been given to securing the plugin yet, nor has it been extensively tested.</p>
