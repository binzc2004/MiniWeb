/**
 * MiniWeb 示例页面的前端脚本。
 * 通过 fetch 调用后端接口，用于在浏览器中直观验证路由与 JSON 转换。
 */
(function () {
    'use strict';

    var output = document.getElementById('output');

    function show(text) {
        output.textContent = text;
    }

    function call(path) {
        show('GET ' + path + '\n…');

        fetch(path, { headers: { 'Accept': 'application/json' } })
            .then(function (resp) {
                return resp.text().then(function (body) {
                    return { status: resp.status, statusText: resp.statusText, body: body };
                });
            })
            .then(function (res) {
                var pretty = res.body;
                try {
                    pretty = JSON.stringify(JSON.parse(res.body), null, 2);
                } catch (e) {
                    // 非 JSON 响应（例如纯文本），保持原样输出
                }
                show('GET ' + path
                    + '\nHTTP ' + res.status + ' ' + res.statusText
                    + '\n\n' + pretty);
            })
            .catch(function (err) {
                show('GET ' + path + '\n请求失败: ' + err);
            });
    }

    var buttons = document.querySelectorAll('button[data-path]');
    for (var i = 0; i < buttons.length; i++) {
        buttons[i].addEventListener('click', function () {
            call(this.getAttribute('data-path'));
        });
    }

    var clock = document.getElementById('clock');
    setInterval(function () {
        clock.textContent = new Date().toLocaleTimeString();
    }, 1000);
})();
