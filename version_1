%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% function out = AOA_moving_target (in)
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Copyright (c) 2014-2017, Infineon Technologies AG
% All rights reserved.
%
% Redistribution and use in source and binary forms, with or without modification,are permitted provided that the
% following conditions are met:
%
% Redistributions of source code must retain the above copyright notice, this list of conditions and the following
% disclaimer.
%
% Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
% disclaimer in the documentation and/or other materials provided with the distribution.
%
% Neither the name of the copyright holders nor the names of its contributors may be used to endorse or promote
% products derived from this software without specific prior written permission.
%
% THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
% INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
% DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE  FOR ANY DIRECT, INDIRECT, INCIDENTAL,
% SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
% SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
% WHETHER IN CONTRACT, STRICT LIABILITY,OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
% OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

% DESCRIPTION:
% This algorithm computes an angle of arrival estimation based on the
% phased difference betweeen two receivers for a moving target. The final 
% plot will show the user the time-domain data received by the antennas, 
% the calculated frequency spectrum of the data and the resulting 
% computation of the angle of arrival and distance between object and radar.

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

% Author: Raghavendran Ulaganathan Vagarappan
% Date: 19.04.2017

% Barbara Lenz on 03.07.2017: updated to new Matlab API

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%% cleanup and init
% Before starting any kind of device the workspace must be cleared and the
% MATLAB Interface must be included into the code. 
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

clc
disp('******************************************************************');
addpath('..\..\RadarSystemImplementation');                           % add Matlab API
clear all 
close all                                                                      % close and delete ports


%% setup object and show properties
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
szPort = findRSPort;                                                           % enter the available ports
oRS = RadarSystem(szPort);                                                     % setup object and connect to board //这两步对应的语法是？oRs拿到的是路径还是什么
disp('oRS object - properties before set block:');
oRS

%% set properties
% Changing some default properties on the board for proper operation of the
% code. 
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

oRS.oEPRadarFMCW.lower_frequency_kHz = oRS.oEPRadarBase.min_rf_frequency_kHz+1e6; % = 57 GHz
oRS.oEPRadarFMCW.upper_frequency_kHz = oRS.oEPRadarBase.min_rf_frequency_kHz+5e6; % = 61 GHz intended?
oRS.oEPRadarFMCW.direction = 0;                                                   % 'up-chirp'                                       
oRS.oEPRadarFMCW.tx_power = 31;                                                   % valid powerrange: 0 - 31
                                                                                  % 10 will set the conducted transmit power to a value of 0 dBm
oRS.oEPRadarSoli.samplerate_Hz = 2000000;                                         % 2 MHz max. sampling rate
oRS.oEPRadarBase.num_chirps_per_frame = 1;                                        % one chirp per radar transmission
oRS.oEPRadarBase.num_samples_per_chirp = 256;                                     % 128 sample points per chirp
oRS.oEPRadarBase.rx_mask = bin2dec('1100');                                       % turn on up to 4 receiverchannels
                                                                                  % every bit represents one receiver, bit mask is oRS.sRXMask=[RX4 RX3 RX2 RX1]
oRS.oEPRadarSoli.tx_mode = 0;                                                     % TX1 transmitter only
%oRS.oEPRadarSoli.tx_mode = 2;                                                    % 2 for two transmitter sequencially

disp('oRS object - properties after set block:');

N=double(oRS.oEPRadarBase.num_samples_per_chirp);                                             
Zeropad=N*4;                                                                   % = Zeropadding to multiple of N where N*4 is good choice
T=double(oRS.oEPRadarBase.chirp_duration_ns)*1e-9;                             % chirp time in seconds
BW=double(oRS.oEPRadarFMCW.upper_frequency_kHz-oRS.oEPRadarFMCW.lower_frequency_kHz)*1e3; % Radar bandwidth in Hz
kf=BW/T;                                                                       % slope rate in Hz/s; 
c0=3e8;                                                                        % speed of light im m/s; 
scWL=c0/(double(oRS.oEPRadarFMCW.upper_frequency_kHz)*1e3);                    % maximum wavelength
scSpace=0.7*scWL;                                                              % spacing between the receivers
xscale=2*kf/c0;                                                                % scaling for the x-axis as a distance in m
fadc =double(oRS.oEPRadarSoli.samplerate_Hz);
xdata=linspace(0,1-1/Zeropad,Zeropad)*fadc/xscale;                             % scaling the x-axis to proper range to frequency
xdata1=linspace(0,1-1/N,N)*fadc/xscale;                                        % scaling for 
 

Hann_window=hann(N,'periodic');                                                % hann(Length of window, 'periodic');
ScaleHannWin = 1/sum(Hann_window);                                             % replaces the scaling of the fft with length of spectrum with sum(window)

%% start Angle of Arrival estimation
% starting the while-loop where the process of AoA is computed and
% collecting raw_data from the board
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

hTime=figure;
while ishandle(hTime)  

 for kk=1:32                                                                   % trigger chirp and collect raw data 32 times                                                       
[c, ~]=oRS.oEPRadarBase.get_frame_data;     
 d(:,kk)=c(:,1);
 d1(:,kk)=c(:,2);
 end
[recent, ~]=oRS.oEPRadarBase.get_frame_data; 
    
c(:,1)=recent(:,1)-mean(d,2);                                                  % remove clutter 
c(:,2)=recent(:,2)-mean(d1,2);                                                 %这一块怎么理解
figure(hTime);
clf
% plot most recent chirp data and chirp data with removed clutter
hold on
subplot(2,2,1)
plot(xdata1,recent(:,1)','r',xdata1,recent(:,2)','b',xdata1,c(:,1)','g',xdata1,c(:,2)','m');
grid on
title('Time domain data')
ylim([0,1])

Nbut=4;                                                                        % DC and zero radial velocity removal 
Wn=.05;
[b,a]=butter(Nbut,Wn,'high');

data1=filter(b,a,c(:,1)');
data2=filter(b,a,c(:,2)');

%% Prepare AoA
% Computing the fast-fourier-transform of the raw data and finding any
% peaks for a region of data, here the data has to be in a range of 0.1 to
% 0.9. Peaks are than used for determing range and signal strength of
% object
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

FFT_dat1=fft(data1.*Hann_window',Zeropad);
scaled_FFT1=db(abs(FFT_dat1(1:length(xdata))*1*ScaleHannWin));


FFT_dat2=fft(data2.*Hann_window',Zeropad);
scaled_FFT2=db(abs(FFT_dat2(1:length(xdata))*1*ScaleHannWin));
 
IF_info=[FFT_dat1' FFT_dat2'];                                                  % Create a matrix with two receivers     
    xmin = 0.1;                                                                 % Minimum range value 
    xmax = 0.9;                                                                 % Maximum range value
    region_of_interest = xmax>xdata & xdata>xmin;                               % Desired range value for peak detection  
    start_bin=find(region_of_interest,1)-1;
    [rvPks,rvLocs] = findpeaks(scaled_FFT1(region_of_interest),'MINPEAKHEIGHT',-90,'MINPEAKDISTANCE',15);      
                                                                                % find local peaks        
    [Tx_scPeakVal,scInd] = max(rvPks);                                          % take only maximum peak           
    TxscPeakBin=rvLocs(scInd);

  
 iter=length(TxscPeakBin);

 
 if(iter==1)   

       Desired_bin = start_bin+TxscPeakBin;                                      % Desired bin
       target_range= xdata(Desired_bin);                                         % Target range value
       target_signal_value=scaled_FFT1(Desired_bin);                             % Target signal level
   
   
  
 %%  Angle of Arrival estimation 
 % AoA is determined by taking the phased difference between the two
 % receivers and calculating the AoA:
 % AoA = arcsin((delta_phi/2pi)*(max. wavelength/spacing))
 %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
 
 
 
 rvPh = unwrap(angle(IF_info(Desired_bin,:)),[]);                                % get phase information from the desired target bin
 rvPhDelt = diff(rvPh);                                                          % get phase difference
 scAlphSin = (rvPhDelt/(2*pi))*(scWL/scSpace);                                   % Angle of arrival formaula
 Angle_of_arrival = asind(scAlphSin);                                            % Angle value in degrees
 
 

 %% Plot the range and angle values
 % Finishing with the last 2 plots, the range and angle values and the
 % the frequency spectrum of the raw data plot
 %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
 
 % plot for angle and distance value
 subplot(2,2,2)
 plot(target_range,Angle_of_arrival,'Marker','o','MarkerSize',10,'MarkerFaceColor','blue','MarkerEdgeColor','green')
 xlim([0.1,max(xdata)/2])
 ylim([-50,50])
 grid on;
 legend(num2str(Angle_of_arrival));
 xlabel('Target range in m')
 ylabel('Azimuthal angle in degrees')
 title('Range angle map')
 
 % frequency spectrum plot
 subplot(2,2,[3,4])
 plot(xdata,scaled_FFT1);
 grid on
 ylim([-120,-10])
 xlabel('Target range in m')
 ylabel('Power spectrum in db')
 title('Frequency domain signal')
 xlim([0.1,max(xdata)/2]) 
 drawnow
 rvLocs=[];
 rvPks=[];
 target_signal_value=[];
 target_range=[];
 Desired_bin=[];
 iter=[];
 scPeakBin=[];
 rvPh = [] ;     
 rvPhDelt = [];                                
 scAlphSin = [];           
 Angle_of_arrival =[];    
 

 
   else       
       subplot(2,2,2)
       Angle_of_arrival=0;
       range=0;
       plot(range,Angle_of_arrival,'Marker','o','MarkerSize',10,'MarkerFaceColor','blue','MarkerEdgeColor','green');
       legend('no target') 
       subplot(2,2,[3,4])
       hold off
   end
end
