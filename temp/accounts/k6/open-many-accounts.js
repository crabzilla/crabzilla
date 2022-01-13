import http from 'k6/http';

export default function () {
  const id = create_UUID()
  const payload = JSON.stringify({cpf: id, name: id});
  const params = { headers: { 'Content-Type': 'application/json' } };
  let r = http.put('http://0.0.0.0:8888/accounts/' + id + '/open' , payload, params);
};

function create_UUID(){
    var dt = new Date().getTime();
    var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = (dt + Math.random()*16)%16 | 0;
        dt = Math.floor(dt/16);
        return (c=='x' ? r :(r&0x3|0x8)).toString(16);
    });
    // console.log(uuid);
    return uuid;
}
