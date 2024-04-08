from flask import Flask, request, jsonify
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = 'uploads/'
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@app.route('/')
def home():
    return 'Welcome to the home page!'

@app.route('/upload', methods=['POST'])
def upload_file():
    if 'file' not in request.files:
        return jsonify({'error': 'No file part'}), 400
    file = request.files['file']
    if file.filename == '':
        return jsonify({'error': 'No selected file'}), 400
    if file and allowed_file(file.filename):
        filename = secure_filename(file.filename)
        file.save(os.path.join(app.config['UPLOAD_FOLDER'], filename))
        return jsonify({'message': 'File uploaded successfully'}), 200
    else:
        return jsonify({'error': 'Invalid file type'}), 400

@app.route('/uploads', methods=['GET'])
def list_files():
    upload_folder = app.config['UPLOAD_FOLDER']
    
    # Check if the upload directory exists, create it if it does not
    if not os.path.exists(upload_folder):
        os.makedirs(upload_folder)

    files = os.listdir(upload_folder)
    images = [f for f in files if f.split('.')[-1].lower() in ALLOWED_EXTENSIONS]
    return jsonify({'images': images}), 200

if __name__ == '__main__':
    app.run(debug=True, host='192.168.203.117', port=5000)
