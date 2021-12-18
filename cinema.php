<?php
$id = $_GET['id'];

$url='https://www.facebook.com/video.php?v='.$id;

$context =array (
    'http' => array(
        'method' => 'GET',
        'header' => "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.47 Safari/537.36",
    ),
);
$context = stream_context_create($context);
$data = file_get_contents($url, false, $context);

function cleanStr($str)
{
    return html_entity_decode(strip_tags($str), ENT_QUOTES, 'UTF-8');
}

function hd_finallink($curl_content)
{

    $regex = '/hd_src:"([^"]+)"/';
    if (preg_match($regex, $curl_content, $match)) {
        
        return $match[1];

    } else {return;}
}

function sd_finallink($curl_content)
{

    $regex = '/sd_src:"([^"]+)"/';
    if (preg_match($regex, $curl_content, $match)) {
        
        return $match[1];

    } else {return;}
}

function getTitle($curl_content)
{
    $title = null;
    if (preg_match('/h2 class="uiHeaderTitle"?[^>]+>(.+?)<\/h2>/', $curl_content, $matches)) {
        $title = $matches[1];
    } elseif (preg_match('/title id="pageTitle">(.+?)<\/title>/', $curl_content, $matches)) {
        $title = $matches[1];
    }
    return cleanStr($title);
}

$hdlink = hd_finallink($data);
$sdlink = sd_finallink($data);
$title = gettitle($data);

$message = array();

if ($sdlink != "") {
    $message = array(
        'status' => 'success',
        'title' => $title,
        'hd' => $hdlink,
        'sd' => $sdlink,

    );
} else {
    $message = array(
        'status' => 'failure',
    );
}
echo json_encode($message,JSON_UNESCAPED_SLASHES);

?>
