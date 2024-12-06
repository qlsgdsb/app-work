from flask import Flask, jsonify, request
import mysql.connector
import requests
import json

app = Flask(__name__)

def get_db_connection():
    return mysql.connector.connect(
        host='localhost',        # 数据库主机地址
        user='root',             # 数据库用户名
        password='1978232861',   # 数据库密码
        database='app_work'      # 数据库名称
    )

@app.route('/init', methods=['GET'])
def init():
    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    cursor.execute("SELECT * FROM contacts")
    contacts = cursor.fetchall()

    cursor.execute("SELECT * FROM messages")
    messages = cursor.fetchall()

    cursor.close()
    conn.close()

    return jsonify({
        'contacts': contacts,
        'messages': messages
    })

@app.route('/chat_service', methods=['POST'])
def chat_service():
    # 获取请求中的 JSON 数据
    data = request.get_json()

    # 处理数据，将其转换为 JSON 字符串格式
    if isinstance(data, list):
        processed_data = json.dumps(data, ensure_ascii=False)

        # 发送处理后的数据到 localhost:7891/chat
        response = requests.post('http://localhost:7891/chat', data=processed_data, headers={'Content-Type': 'application/json'})

        # 尝试解析响应数据
        try:
            response_json = response.json()
            # 返回解码后的响应数据
            return jsonify({
                'status_code': response.status_code,
                'response_data': response_json
            })
        except ValueError:
            # 如果响应不是 JSON 格式，直接返回原始响应
            return jsonify({
                'status_code': response.status_code,
                'response_data': response.text
            })

    else:
        return jsonify({'error': 'Invalid input format'}), 400

if __name__ == '__main__':
    app.run(debug=True, port=7892)

#测试命令
#curl -X POST -H "Content-Type: application/json; charset=utf-8" -d "[\"丁真\",\"我好得很\"]" http://localhost:7892/chat_service