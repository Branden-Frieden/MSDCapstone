function handleErrorCB( event ){
    console.log( "error with socket");
}

function handleConnectCB( event ){
    console.log('web socket connected');
}

function handleCloseCB( event ){
    let leaveMessage = document.createElement('p');
    ws.send( "leave " + connectedName + " " + connectedRoom);
    connected = false;
}

function handleMessageFromWsCB( event ){
    let msg = event.data;
    console.log( "got reply from socket");
    let object = JSON.parse( msg );

    let type = object.type;
    let user = object.user;
    let room = object.room;
    let message = object.message

    console.log("received from server: " + event.data);

    if(type == "message"){
        let userMessage = document.createElement('p');
        userMessage.innerHTML = "<b>" + user + ":</b> " + message;
        messages.appendChild( userMessage );
        messages.scrollTop = messages.scrollHeight;
    }
    else if( type == "join" ){
        let userName = document.createElement( 'p' );
        userName.id = user;
        userName.innerText = user;
        users.appendChild( userName );

        let joinMessage = document.createElement('p');
        joinMessage.innerHTML =  "<b><i>" + user + " Joined The Room </i></b>";
        messages.appendChild( joinMessage );
    }
    else if( type == "leave" ){

        let leaveMessage = document.createElement('p');
        leaveMessage.innerHTML =  "<b><i>" + user + " Left The Room </i></b>";
        messages.appendChild( leaveMessage );

        let leaver = document.getElementById( user );
        leaver.remove();
    }
    console.log(msg);
}


/*
function handleKeyPressCB( event ){


    if( event.keyCode == 13) {
        event.preventDefault();

        if( !connected ){
            let name = nameBox.value;
            let room = roomBox.value;
            if(/\s/.test(room) || room != room.toLowerCase() || room.includes(" ")){
                return;
            }

            if( name === '' || room === ''){
                console.log('invalid connection');
                return;

            }
            ws.send( "join " + name + " " + room );
            connectedRoom = room;
            connectedName = name;
            connected = true;
        }

        let msg = messageBox.value;

        if(msg.length == 0){
            return;
        }

        if(connected) {
            console.log('web socket sent');

            console.log(connectedName + " " + msg);
            ws.send(connectedName + " " + msg);
            event.target.select();
        }
    }
}
*/

//--------------------------------- helper functions-------------------------//

// fades flash on video for image taking effect
function fadeOverlayBackground(){
  if(overlayOpacity <= 0){
    clearInterval(fadeOverlayTimer);
  }
  else{
    overlayOpacity = overlayOpacity - .02;
    videoOverlay.style.backgroundColor = "rgba(255,255,255,"+ overlayOpacity + ")";
  }
}

// sends image to server for analysis
function sendImage( image ){
  ws.send(image);
  console.log(image)
}

// countdown to photo
function countdown(){
    videoOverlay.innerText = Math.round(parseFloat(videoOverlay.innerText) * 10 - 1) / 10
    if(videoOverlay.innerText * 10 - parseInt(videoOverlay.innerText) * 10 == 0){
      videoOverlay.innerText = videoOverlay.innerText + ".0"
    }

  if(videoOverlay.innerText <= 0) {
    singleButtonCB();
  }
}

//-------------------------------- event Callback functions-------------------//
// takes single image for server to analyze
function singleButtonCB( event ){
  overlayOpacity = .8
  videoOverlay.style.backgroundColor = "rgba(255,255,255,"+ overlayOpacity + ")";
  fadeOverlayTimer = setInterval(fadeOverlayBackground, 5)
  videoOverlay.innerText = intervalBox.value + ".0"

  ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
  var frameDataURL = canvas.toDataURL('image/png');
  sendImage(frameDataURL)
}

// starts timer to take image every few seconds
function startButtonCB( event ){
  stopButtonCB();
  videoOverlay.innerText = intervalBox.value + ".0"
  countdownTimer = setInterval(countdown, 100)
}

// stops timers taking images
function stopButtonCB( event ){
  clearInterval(countdownTimer);
  videoOverlay.innerText = ""
}

// copies editable text area to local clipboard
function copyButtonCB( event ){
  navigator.clipboard.writeText(textBox.innerText);

}

// removes all text from editable text area
function clearButtonCB( event ){
  textBox.innerText = ""
}

//----------------------------- initialization -----------------------------//
  var video = document.querySelector("#videoElement");

  if (navigator.mediaDevices.getUserMedia) {
    navigator.mediaDevices.getUserMedia({ video: true })
      .then(function (stream) {
        video.srcObject = stream;

      })
      .catch(function (err0r) {
        console.log("Something went wrong!");
      });
  }

  var canvas = document.createElement('canvas');
  var ctx = canvas.getContext('2d');


  var countdownTimer;
  var fadeOverlayTimer;

  var overlayOpacity = 0;
  let connected = false;

  // set element variables
  let cameraBox = document.getElementById("videoElement")
  let intervalBox = document.getElementById("intervalTime")
  let countdownBox = document.getElementById("countdownDisplay")
  let singleButton = document.getElementById("singleButton")
  let startButton = document.getElementById("startButton")
  let stopButton = document.getElementById("stopButton")
  let letterDisplayPar = document.getElementById("letterDisplay")
  let letterConfidencePar = document.getElementById("letterConfidence")
  let copyButton = document.getElementById("copyButton")
  let clearButton = document.getElementById("clearButton")
  let videoOverlay = document.getElementById("videoOverlay")
  let textBox = document.getElementById("textBox")

  // create callback event Listeners
  singleButton.addEventListener("click", singleButtonCB)
  startButton.addEventListener("click", startButtonCB)
  stopButton.addEventListener("click", stopButtonCB)
  copyButton.addEventListener("click", copyButtonCB)
  clearButton.addEventListener("click", clearButtonCB)


let ws = new WebSocket("ws://localhost:8080");
ws.onopen = handleConnectCB;
ws.onmessage = handleMessageFromWsCB;
ws.onclose = handleCloseCB;
ws.onerror = handleErrorCB;

window.onbeforeunload = handleCloseCB;
