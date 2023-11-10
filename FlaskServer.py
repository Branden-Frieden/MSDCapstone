from flask import Flask, render_template, session, copy_current_request_context, request
from flask_socketio import SocketIO, emit, disconnect
from threading import Lock
import torch
from PIL import Image  
import torchvision.transforms as transforms
from torch import nn
import torch.nn.functional as F
import base64
import io
from torchvision.utils import save_image
import numpy as np
import mediapipe as mp
import cv2


mp_drawing = mp.solutions.drawing_utils
mp_drawing_styles = mp.solutions.drawing_styles
mp_hands = mp.solutions.hands


class_names = ["0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"]

class Net(nn.Module):
  def __init__(self, outputs) -> None:
    super().__init__()
    conv1 = 16
    self.conv_block_1 = nn.Sequential(
        nn.Conv2d(in_channels=3,
                  out_channels=conv1,
                  kernel_size=3,
                  stride=1,
                  padding=0),
        nn.ReLU(),
        nn.Conv2d(in_channels=conv1,
                  out_channels=conv1,
                  kernel_size=3,
                  stride=1,
                  padding=0),
        nn.ReLU(),
        nn.MaxPool2d(kernel_size=2,
                      stride=2)
    )
    self.conv_block_2 = nn.Sequential(
            nn.Conv2d(in_channels=conv1,
                      out_channels=conv1,
                      kernel_size=3,
                      stride=1,
                      padding=0),
            nn.ReLU(),
            nn.Conv2d(in_channels=conv1,
                      out_channels=conv1,
                      kernel_size=3,
                      stride=1,
                      padding=0),
            nn.ReLU(),
            nn.MaxPool2d(kernel_size=2,
                        stride=2)
        )
    self.classifier_layer = nn.Sequential(
        nn.Flatten(),
        nn.Linear(in_features=self.calculate_fc_input_size(),
                  out_features=outputs)
    )

  def forward(self, x):
    x = self.conv_block_1(x)
    #print(x.shape)
    x = self.conv_block_2(x)
    #print(x.shape)
    x = self.classifier_layer(x)
    #print(x.shape)
    return x
    
  def num_flat_features(self, x):
      size = x.size()[1:]  # all dimensions except the batch dimension
      num_features = 1
      for s in size:
          num_features *= s
      return num_features
  def calculate_fc_input_size(self):
      # Compute the input size for the fully connected layer
      dummy_input = torch.randn(1, 3, 128, 128)  #input size of 64 x 64
      x = self.conv_block_2(self.conv_block_1(dummy_input))
      return x.view(x.size(0), -1).size(1)


def edgedetect (channel):
    sobelX = cv2.Sobel(channel, cv2.CV_16S, 1, 0)
    sobelY = cv2.Sobel(channel, cv2.CV_16S, 0, 1)
    sobel = np.hypot(sobelX, sobelY)

    sobel[sobel > 255] = 255
    return sobel

def findSignificantContours (img, edgeImg):
    contours, heirarchy = cv2.findContours(edgeImg, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)

    # Find level 1 contours
    level1 = []
    for i, tupl in enumerate(heirarchy[0]):
        # Each array is in format (Next, Prev, First child, Parent)
        # Filter the ones without parent
        if tupl[3] == -1:
            tupl = np.insert(tupl, 0, [i])
            level1.append(tupl)

    # From among them, find the contours with large surface area.
    significant = []
    tooSmall = edgeImg.size * 5 / 100 # If contour isn't covering 5% of total area of image then it probably is too small
    for tupl in level1:
        contour = contours[tupl[0]];
        area = cv2.contourArea(contour)
        biggest_area = 0
        if area > tooSmall:
            significant.append([contour, area])

    significant.sort(key=lambda x: x[1])
    #print ([x[1] for x in significant]);
    return [x[0] for x in significant];    

def process_image(image):

    height = np.asanyarray(image).shape[0]
    width = np.asarray(image).shape[1]
    try:
        #  ------------------------------------------------------------------------------get hand locations
        minx, miny, maxx, maxy = float('inf'), float('inf'), -float('inf'), -float('inf')
        with mp_hands.Hands(
            static_image_mode=True,
            max_num_hands=1,
            min_detection_confidence=0.5) as hands:
            results = hands.process(cv2.cvtColor(np.asarray(image), cv2.COLOR_BGR2RGB))
            if results.multi_hand_world_landmarks:
                hand_world_landmarks = results.multi_hand_landmarks[0]  # Assuming you want to process the first detected hand
                for point in hand_world_landmarks.landmark:
                    x = int(point.x * width)
                    y = int(point.y * height)
                    # Update min and max values while ensuring they are within the image bounds
                    minx = min(minx, x)
                    miny = min(miny, y)
                    maxx = max(maxx, x)
                    maxy = max(maxy, y)

        x_size = maxx - minx
        y_size = maxy - miny
        size_diff = (x_size - y_size) / 2

        # crop image to make it a square with both sides equal to the longest side of the hand
        if(size_diff > 0):
            print("x>y: ", size_diff)
            image = image.crop((minx - 20, miny - size_diff - 20, maxx + 20, maxy + size_diff + 20))
        else:
            print("y>x: ", size_diff)
            image = image.crop((minx + size_diff - 20, miny - 20 , maxx - size_diff + 20, maxy + 20))
        
    except:
        print("Failed to get hand")


    #----------------------------------------------------------------------------------------#

    image = np.asarray(image)

    image = image.copy()
    top, bottom, left, right = 0, 10, 0, 0
    image = cv2.copyMakeBorder(image, top, bottom, left, right, cv2.BORDER_CONSTANT, value=0)

    blurred = cv2.GaussianBlur(image, (5, 5), 0) 
    edgeImg = np.max( np.array([ edgedetect(blurred[:,:, 0]), edgedetect(blurred[:,:, 1]), edgedetect(blurred[:,:, 2]) ]), axis=0 )
    mean = np.mean(edgeImg)
    edgeImg[edgeImg <= mean] = 0

    edgeImg_8u = np.asarray(edgeImg, np.uint8)
    significant = findSignificantContours(image, edgeImg_8u)

    mask = edgeImg.copy()
    mask[mask > 0] = 0
    cv2.fillPoly(mask, significant, 255)
    mask = np.logical_not(mask)

    
    image[mask] = 0

    image = Image.fromarray(image)


    if image.mode == 'RGBA':
        image = image.convert('RGB')
    image.save("image.png")


    # Preprocess image for model
    transformation = transforms.Compose([
    # Resize our images to 64 x 64
    transforms.Resize(size=(128, 128)),
    #Turn image into torch.Tensor
    transforms.ToTensor()
    ])
    image_tensor = transformation(image).unsqueeze(0)
    
    return image_tensor



async_mode = None
app = Flask(__name__)
app.config['SECRET_KEY'] = 'secret!'
socket_ = SocketIO(app, async_mode=async_mode)
thread = None
thread_lock = Lock()

if __name__ == '__main__':
    app.run(debug=False, port=8001)

model = Net(36)
model.load_state_dict(torch.load("model"))
model.eval()

@app.route('/')
def index():
    return render_template('index.html', async_mode=socket_.async_mode)

@socket_.on('image')
def handle_image(image_data_):
    print('got image data')
    image_data_ = image_data_[image_data_.index(",") + 1:]

    image_data_ = base64.b64decode(image_data_)

    image = Image.open(io.BytesIO(image_data_))
    print(image.size)
    print(np.asarray(image).shape)




    image_tensor = process_image(image)
    print(image_tensor.shape)


    with torch.no_grad(): 
        # put image through model
        output = model(image_tensor)

        # Get class probabilities
        probabilities = torch.nn.functional.softmax(output, dim=1)
        probabilities = probabilities.detach().numpy()[0]
        print("probabilities: ", probabilities)

        # Get the index of the highest probability
        class_index = probabilities.argmax()
        print("class_index: ", class_index)

        # Get the predicted class and probability
        predicted_class = class_names[class_index]
        probability = probabilities[class_index]

        # Sort class probabilities in descending order
        # class_probs = list(zip(class_names, probabilities))
        # class_probs.sort(key=lambda x: x[1], reverse=True)

        print("{\"letter\" :\"" + predicted_class + "\",\"confidence\" :\"" + str(probability) + "\"}")
        emit('image', "{\"letter\" :\"" + predicted_class + "\",\"confidence\" :\"" + str(probability) + "\"}")


@socket_.on('my_event', namespace='/test')
def test_message(message):
    session['receive_count'] = session.get('receive_count', 0) + 1
    emit('my_response',
         {'data': message['data'], 'count': session['receive_count']})


@socket_.on('my_broadcast_event', namespace='/test')
def test_broadcast_message(message):
    session['receive_count'] = session.get('receive_count', 0) + 1
    emit('my_response',
         {'data': message['data'], 'count': session['receive_count']},
         broadcast=True)


@socket_.on('disconnect_request', namespace='/test')
def disconnect_request():
    @copy_current_request_context
    def can_disconnect():
        disconnect()

    session['receive_count'] = session.get('receive_count', 0) + 1
    emit('my_response',
         {'data': 'Disconnected!', 'count': session['receive_count']},
         callback=can_disconnect)


if __name__ == '__main__':
    socket_.run(app, debug=True)