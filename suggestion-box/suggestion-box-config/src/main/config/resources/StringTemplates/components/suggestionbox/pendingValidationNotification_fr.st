<div style="font-family:Arial;font-size:11px">
L'idée <b>$title$</b> doit être validée.

$if(notification_receiver_users)$
Ce message a été envoyé aux utilisateurs : $notification_receiver_users$
$endif$

$if(notification_receiver_groups)$
  $if(!notification_receiver_users)$
  Ce message a été envoyé
  $endif$
 aux groupes : $notification_receiver_groups$
$endif$
</div>